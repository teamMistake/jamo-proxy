package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.database.Chat
import io.teammistake.chatjamo.database.ChatMessage
import io.teammistake.chatjamo.database.ChatRepository
import io.teammistake.chatjamo.exceptions.PermissionDeniedException
import io.teammistake.suzume.exception.NotFoundException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PromptingService {
    @Autowired
    lateinit var chatRepository: ChatRepository;

    @Autowired
    lateinit var suzumeService: SuzumeService;

    suspend fun sendChat(chatId: String, prompt: String, chosenReqId: String? = null): ChatMessage {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");
        if (chat.generating) throw PermissionDeniedException("Response is being generated")
        chat.generating = true
        chat = chatRepository.save(chat).awaitSingle()
        try {
            chat = chosenReqId?.let { chooseResponse(chatId, it) } ?: chat

            TODO()
        } finally {
            chat.generating = false
            chatRepository.save(chat).awaitSingle()
        }
    }
    suspend fun regenerateChat(chatId: String) {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");
        if (chat.generating) throw PermissionDeniedException("Response is being generated")
        chat.generating = true
        chat = chatRepository.save(chat).awaitSingle()
        try {
            var message = chat.messages.last()

            TODO()
        } finally {
            chat.generating = false
            chatRepository.save(chat).awaitSingle()
        }
    }

    suspend fun chooseResponse(chatId: String, chosenReqId: String ): Chat {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");

        val message = chat.messages.last() // can only choose response in last request

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