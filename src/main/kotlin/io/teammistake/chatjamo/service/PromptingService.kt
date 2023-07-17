package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.database.*
import io.teammistake.chatjamo.dto.*
import io.teammistake.chatjamo.exceptions.APIErrorException
import io.teammistake.chatjamo.exceptions.PermissionDeniedException
import io.teammistake.chatjamo.exceptions.NotFoundException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.apache.logging.log4j.message.FlowMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.minutes

@Service
class PromptingService {
    @Autowired
    lateinit var chatRepository: ChatRepository;

    @Autowired
    lateinit var suzumeService: SuzumeService;


    class SuzumeDone(val cause: Exception?): SuzumeStreamingResponse();

    suspend fun log(chatId: String, messageId: String, apiInferenceRequest: APIInferenceRequest, resp: Flow<SuzumeStreamingResponse>, type: ResponseType, uid: String?) {
            var state = true;
            var last: String = "";
            val job = CoroutineScope(SupervisorJob()).async {
                val header = resp
                    .map { if(it is APIError) throw APIErrorException(it,chatId,messageId) else it  }
                    .takeWhile { it !is SuzumeDone }.first { it is APIResponseHeader } as APIResponseHeader
                val body = resp
                    .map { if(it is APIError) throw APIErrorException(it,chatId,messageId) else it  }
                    .takeWhile { it !is SuzumeDone }
                    .filterIsInstance(InferenceResponse::class)

                val suzumeResp = body.transformWhile {
                    emit(it)
                    if (it.respFull != null)
                        last = it.respFull ?: ""
                    val toReturn = state;
                    state = !it.eos;
                    toReturn
                }.timeout(1.minutes).last()

                return@async ChatMessageResponse(
                    reqId = header.reqId,
                    model = header.model!!,
                    text = suzumeResp.respFull ?: "",
                    selected = false,
                    feedback = null,
                    error = suzumeResp.error,
                    type = type,
                    hyperParameter = listOf(
                        "topK" to apiInferenceRequest.topK.toString(),
                        "temperature" to apiInferenceRequest.temperature.toString(),
                        "maxToken" to apiInferenceRequest.maxToken.toString()
                    ).toMap().toMutableMap()
                )
            }
            var respEntity: ChatMessageResponse;
            try {
                respEntity = job.await()
            } catch (e: APIErrorException) {
                e.printStackTrace()
                respEntity = ChatMessageResponse(
                    reqId = e.err.reqId,
                    model = e.err.request?.model ?: "",
                    text = last,
                    selected = false,
                    feedback = null,
                    error = e.message,
                    type = type,
                    hyperParameter = listOf(
                        "topK" to apiInferenceRequest.topK.toString(),
                        "temperature" to apiInferenceRequest.temperature.toString(),
                        "maxToken" to apiInferenceRequest.maxToken.toString()
                    ).toMap().toMutableMap()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                respEntity = ChatMessageResponse(
                    reqId = null,
                    model = apiInferenceRequest.model,
                    text = last,
                    selected = false,
                    feedback = null,
                    error = e.message,
                    type = type,
                    hyperParameter = listOf(
                        "topK" to apiInferenceRequest.topK.toString(),
                        "temperature" to apiInferenceRequest.temperature.toString(),
                        "maxToken" to apiInferenceRequest.maxToken.toString()
                    ).toMap().toMutableMap()
                )
            }
            val chat = chatRepository.findById(chatId).awaitSingle();
            val message = chat.messages.findLast { it.messageId == messageId } ?: throw IllegalStateException("Message not found?? $chatId / $messageId")

            message.resp.add(respEntity)
            chatRepository.save(chat).awaitSingle()

        if (uid != null) {
            userService.incrementReceivedMessages(uid, respEntity.text.length)
            leaderboardService.addScore(uid, respEntity.text.length.toDouble())
        }
    }

    @Autowired
    lateinit var leaderboardService: LeaderboardService
    @Autowired
    lateinit var userService: UserService

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun requestSuzume(chatId: String, messageId: String, apiInferenceRequest: APIInferenceRequest, responseType: ResponseType, uid: String?): Pair<APIResponseHeader, Flow<InferenceResponse>>  {
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        val resp = suzumeService.generateResponse(apiInferenceRequest)
                .onCompletion { cause -> if (cause == null) emit(SuzumeDone(null))}
                .shareIn(coroutineScope, SharingStarted.Lazily, 10)

        GlobalScope.launch {
           log(chatId, messageId, apiInferenceRequest, resp, responseType, uid)
        }


        val header = resp
            .map { if(it is APIError) throw APIErrorException(it, chatId, messageId) else it  }
            .takeWhile { it !is SuzumeDone }
            .timeout(1.minutes)
            .first { it is APIResponseHeader } as APIResponseHeader
        val body = resp
            .map { if(it is APIError) throw APIErrorException(it, chatId, messageId) else it  }
            .takeWhile { it !is SuzumeDone }
            .filterIsInstance(InferenceResponse::class)

        return Pair(header, body)
    }

    fun List<ChatMessage>.buildContext() = flatMap {
            val botMessage = if (it.resp.size == 1) it.resp.last() else it.resp.find { it.selected } ?:  throw IllegalStateException("No response chosen")

            listOf(
                ContextPart(ContextType.HUMAN, it.req),
                ContextPart(ContextType.BOT, botMessage.text)
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun sendChatStreaming(chatId: String, prompt: String, chosenReqId: String? = null): Flow<MessageEvent>  {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");
        if (chat.generating) throw PermissionDeniedException("Response is being generated")
        if (chat.messages.size > 5 && chat.userId == null) throw PermissionDeniedException("Can only send 5 messages to unbound chat")
        chat.generating = true
        chat = chatRepository.save(chat).awaitSingle()

        val uid = getUser()?.getUserId()
        if (uid != null) {
            leaderboardService.addScore(uid, prompt.length.toDouble())
            userService.incrementSentMessages(uid, prompt.length)
        }
        try {
            chat = chosenReqId?.let { chooseResponse(chatId, chat.messages.last().messageId ?: "", it) } ?: chat

            val newMessage = ChatMessage(UUID.randomUUID().toString(), prompt, resp = mutableListOf(), experiment = Normal())

            val contexts = chat.messages.buildContext()
            chat.messages.add(newMessage)
            chat = chatRepository.save(chat).awaitSingle()

            val jobs: MutableList<Deferred<Pair<APIResponseHeader, Flow<MessageEvent>>>> = mutableListOf();

            run {
                val apiCallScope = CoroutineScope(coroutineContext + SupervisorJob());
                val job = apiCallScope.async {
                    val req = APIInferenceRequest(
                        prompt,
                        contexts,
                        maxToken = 512,
                        stream = true,
                        model = "prod_a",) // TODO: hardcoded
                    val (header, resp) = requestSuzume(chatId, newMessage.messageId!!, req, ResponseType.PLAIN, uid);

                    return@async Pair(header, resp.map {
                        MessageEvent(
                            ResponseGenerationEvent(
                                chatId,
                                newMessage.messageId!!,
                                header.reqId!!,
                                it
                            )
                        )
                    })
                }
                jobs.add(job)
            }

            try {
                jobs.awaitAll()
            } catch (_: Exception) {}


            val failed = jobs.filter { it.isCancelled }
            val successful = jobs.filter { it.isCompleted && !it.isCancelled }

            val flow = flowOf(MessageEvent(MessageCreationEvent(chatId, newMessage)),
                MessageEvent(ResponseIdsEvent(successful.map { it.await().first }))
            ).onCompletion {
                emitAll(merge(
                    *successful.map { it.await().second }.toTypedArray(),
                    *failed.map {
                        val err = it.getCompletionExceptionOrNull()
                        if (err is APIErrorException) flowOf(MessageEvent(ResponseGenerationError(err.chatId, err.messageId, err.err.reqId, err.message, err.message)))
                        else flowOf(MessageEvent(ResponseGenerationError(chatId, newMessage.messageId, null, err?.message ?: "-empty-", "")))}.toTypedArray()
                ))
            }
            return flow;
        } finally {
                chat.generating = false
                chatRepository.save(chat).awaitSingle()
        }
    }
    suspend fun regenerateChatStreaming(chatId: String): Flow<MessageEvent> {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");
        if (chat.generating) throw PermissionDeniedException("Response is being generated")
        if (chat.userId == null) throw PermissionDeniedException("Can not regenerate in unbound chat")
        chat.generating = true
        chat = chatRepository.save(chat).awaitSingle()
        try {
            val message = chat.messages.last()
            if (message.messageId == null) throw PermissionDeniedException("Can not generate response on message with null id")
            val req = APIInferenceRequest(
                message.req,
                chat.messages.filter { it != message }.buildContext(),
                maxToken = 512,
                stream = true,
                model = "prod_a", // TODO: hardcoded
            )

            val request = CoroutineScope(coroutineContext + SupervisorJob()).
                async { requestSuzume(chatId, message.messageId !!, req, ResponseType.REGENERATED, getUser()?.getUserId()) }


            try {
                val (header, resp) = request.await()
                return flowOf(
                    MessageEvent(MessageCreationEvent(chatId, message)),
                    MessageEvent(ResponseIdsEvent(listOf(header)))
                ).onCompletion {
                    emitAll(merge(resp.map {
                        MessageEvent(
                            ResponseGenerationEvent(
                                chatId,
                                message.messageId!!,
                                header.reqId!!,
                                it
                            )
                        )
                    }))
                }
            } catch (e: APIErrorException) {
                return flowOf(
                    MessageEvent(MessageCreationEvent(chatId, message)),
                    MessageEvent(ResponseIdsEvent(listOf(APIResponseHeader(e.err.reqId, e.err.request?.model)))),
                    MessageEvent(ResponseGenerationError(chatId, message.messageId, e.err.reqId, e.message, e.err))
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return flowOf(
                    MessageEvent(MessageCreationEvent(chatId, message)),
                    MessageEvent(ResponseIdsEvent(listOf())),
                    MessageEvent(ResponseGenerationError(chatId, message.messageId, null, e.message, null))
                )
            }
        } finally {
            chat = chatRepository.findById(chat.chatId).awaitSingle()
            chat.generating = false
            chatRepository.save(chat).awaitSingle()
        }
    }

    suspend fun chooseResponse(chatId: String, messageId: String, chosenReqId: String ): Chat {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");

        val message = chat.messages.last() // can only choose response in last request
        if (message.messageId != messageId) throw PermissionDeniedException("Can only choose in last message")

        val req = message.resp.find {
            it.reqId == chosenReqId
        } ?: throw NotFoundException("Response $chosenReqId not found") // just ensuring it exists.


        message.resp.forEach {
            it.selected = false
        }
        req.selected = true

        chat = chatRepository.save(chat).awaitSingle()
        return chat
    }


    suspend fun rateResponse(chatId: String, messageId: String, reqId: String, stars: Int) {
        require(stars in intArrayOf(0,1,2,3,4,5)) {"Star must be in 0 through 5"}

        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");

        val message = chat.messages
            .findLast { it.messageId == messageId } // ew linear search... But I don't think anyone is gonna have chat thing 1m message long. lol. hopefully.
            // also a heuristic: user is most likely to rate response at the end
            ?: throw NotFoundException("Message $messageId not found")

        val req = message.resp.find {
            it.reqId == reqId
        } ?: throw NotFoundException("Response $reqId not found") // just ensuring it exists.

        suzumeService.feedback(reqId, stars / 5.0);

        req.feedback = stars;
        chat = chatRepository.save(chat).awaitSingle();
    }
}