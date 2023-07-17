package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.database.*
import io.teammistake.chatjamo.dto.*
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

    suspend fun requestSuzume(chatId: String, messageId: String, apiInferenceRequest: APIInferenceRequest): Pair<APIResponseHeader, Flow<InferenceResponse>>  {
        println("Requesting... $chatId $messageId $apiInferenceRequest")
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val resp = suzumeService.generateResponse(apiInferenceRequest)
            .shareIn(coroutineScope, SharingStarted.Lazily, 10)

        val coroutineScope2 = CoroutineScope(Dispatchers.IO)
        val header = resp.filter { it is APIResponseHeader }.map { it as APIResponseHeader }.first()
        val body = resp.filter { it is InferenceResponse }.map { it as InferenceResponse }.shareIn(coroutineScope2, SharingStarted.Lazily, 9999);
        println("Received header $header")
        with(CoroutineScope(Dispatchers.IO)) {
            launch {
                var state = true;
                var last: String = "";
                var resp: ChatMessageResponse;
                try {
                    val suzumeResp = body.transformWhile {
                        emit(it)
                        if (it.respFull != null)
                            last = it.respFull ?: ""
                        val toReturn = state;
                        state = !it.eos;
                        toReturn
                    }.timeout(5.minutes).last()

                    resp = ChatMessageResponse(
                        reqId = header.reqId,
                        model = header.model,
                        text = suzumeResp.respFull ?: "",
                        selected = false,
                        feedback = null,
                        error = suzumeResp.error,
                        type = ResponseType.PLAIN,
                        hyperParameter = listOf(
                            "topK" to apiInferenceRequest.topK.toString(),
                            "temperature" to apiInferenceRequest.temperature.toString(),
                            "maxToken" to apiInferenceRequest.maxToken.toString()
                        ).toMap().toMutableMap()
                    )
                } catch (e: TimeoutCancellationException) {
                    resp = ChatMessageResponse(
                            reqId = header.reqId,
                            model = header.model,
                            text = last,
                            selected = false,
                            feedback = null,
                            error = e.message,
                            type = ResponseType.PLAIN,
                            hyperParameter = listOf(
                                "topK" to apiInferenceRequest.topK.toString(),
                                "temperature" to apiInferenceRequest.temperature.toString(),
                                "maxToken" to apiInferenceRequest.maxToken.toString()
                            ).toMap().toMutableMap()
                        )
                }

                val chat = chatRepository.findById(chatId).awaitSingle();
                val message = chat.messages.findLast { it.messageId == messageId } ?: throw IllegalStateException("Message not found?? $chatId / $messageId")
                message.resp.add(resp)
                chatRepository.save(chat).awaitSingle()
            }
        }

        return Pair(header, body)
    }

    suspend fun sendChatStreaming(chatId: String, prompt: String, chosenReqId: String? = null): Flow<MessageEvent>  {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");
        if (chat.generating) throw PermissionDeniedException("Response is being generated")
        chat.generating = true
        chat = chatRepository.save(chat).awaitSingle()
        try {
            if (chat.messages.size > 5 && chat.userId == null) throw PermissionDeniedException("Can only send 5 messages to unbound chat")

            chat = chosenReqId?.let { chooseResponse(chatId, chat.messages.last().messageId ?: "", it) } ?: chat

            val newMessage = ChatMessage(UUID.randomUUID().toString(), prompt, resp = mutableListOf(), experiment = Normal())


            val events: MutableList<Flow<MessageEvent>> = mutableListOf()
            val headers: MutableList<APIResponseHeader> = mutableListOf()
            run {
                val req = APIInferenceRequest(
                    prompt,
                    chat.messages
                        .flatMap {
                            val botMessage = if (it.resp.size == 1) it.resp.last() else it.resp.find { it.selected } ?:  throw IllegalStateException("No response chosen")

                            listOf(
                                ContextPart(ContextType.HUMAN, it.req),
                                ContextPart(ContextType.BOT, botMessage.text)
                            )
                        }
                    ,
                    maxToken = 512,
                    stream = true,
                    model = "prod-a", // TODO: hardcoded
                )
                val (header, resp) = requestSuzume(chatId, newMessage.messageId !!, req);
                var state = true
                headers.add(header)
                events.add(resp.transformWhile {
                    emit(it)
                    val toReturn = state;
                    state = !it.eos;
                    toReturn
                }.map { MessageEvent(ResponseGenerationEvent(chatId, newMessage.messageId !! , header.reqId, it)) })
            }

            chat.messages.add(newMessage)
            chat = chatRepository.save(chat).awaitSingle()



            val flow = flowOf(MessageEvent(MessageCreationEvent(chatId, newMessage)),
                MessageEvent(ResponseIdsEvent(headers))
            ).flatMapConcat { merge(*events.toTypedArray()) }
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
                chat.messages
                    .flatMap {
                        val botMessage = if (it.resp.size == 1) it.resp.last() else it.resp.find { it.selected } ?:  throw IllegalStateException("No response chosen")

                        listOf(
                            ContextPart(ContextType.HUMAN, it.req),
                            ContextPart(ContextType.BOT, botMessage.text)
                        )
                    }
                ,
                maxToken = 512,
                stream = true,
                model = "prod-a", // TODO: hardcoded
            )
            val (header, resp) = requestSuzume(chatId, message.messageId !!, req);
            chat = chatRepository.save(chat).awaitSingle()

            var state = true
            return flowOf(MessageEvent(MessageCreationEvent(chatId, message)),
                MessageEvent(ResponseIdsEvent(listOf(header)))).flatMapConcat { merge(resp.transformWhile {
                emit(it)
                val toReturn = state;
                state = !it.eos;
                toReturn
            }.map { MessageEvent(ResponseGenerationEvent(chatId, message.messageId !! , header.reqId, it)) }) }
        } finally {
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