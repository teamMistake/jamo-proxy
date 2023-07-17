package io.teammistake.chatjamo.service

import io.teammistake.chatjamo.security.UserPrincipal
import io.teammistake.chatjamo.database.Chat
import io.teammistake.chatjamo.database.ChatRepository
import io.teammistake.chatjamo.database.LightChat
import io.teammistake.chatjamo.exceptions.PermissionDeniedException
import io.teammistake.chatjamo.exceptions.NotFoundException
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*
import kotlin.coroutines.coroutineContext

fun UserPrincipal.getUserId(): String {
    return this.user
}


fun Chat.canView(user: UserPrincipal?): Boolean {
    if (this.shared) return true;
    if (this.userId == null) return true;
    if (this.userId == user?.getUserId()) return true;
    return false;
}

fun Chat.isOwner(user: UserPrincipal?): Boolean {
    if (this.userId == null) return true;
    if (this.userId == user?.getUserId()) return true;
    return false;
}

suspend fun getUser(): UserPrincipal? {
    val ctx = coroutineContext[ReactorContext.Key]?.context?.get<Mono<SecurityContext>>(SecurityContext::class.java)?.asFlow()?.singleOrNull()
    val user = ctx?.authentication?.principal as UserPrincipal?;
    return user;
}

@Service
class ChatService {
    @Autowired lateinit var chatRepository: ChatRepository;


    @Autowired lateinit var reactiveMongoTemplate: ReactiveMongoTemplate;

    @PreAuthorize("isAuthenticated()")
    suspend fun getChatsByMe(): Flux<LightChat> {
        val query = Query(Criteria.where("userId").`is`(getUser()?.getUserId()))
        return reactiveMongoTemplate.find(query, LightChat::class.java);
    }

    suspend fun createChat(): Chat {
        val uid = getUser()?.getUserId();
        var chat = Chat(
            chatId = UUID.randomUUID().toString(),
            userId = uid,
            shared = false,
            messages = mutableListOf(),
            creationTimestamp = Instant.now()
        );
        chat = chatRepository.save(chat).awaitSingle();
        return chat;
    }

    suspend fun deriveChatFrom(chatId: String, messageId: String, reqId: String): Chat {
        val chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.canView(getUser())) throw PermissionDeniedException("Can not view chat");

        val messageIdx = chat.messages
            .indexOfLast { it.messageId == messageId } // ew linear search... But I don't think anyone is gonna have chat thing 1m message long. lol. hopefully.
        if (messageIdx == -1) throw NotFoundException("Message $messageId not found")

        val message = chat.messages[messageIdx];
        val req = message.resp.find {
            it.reqId == reqId
        } ?: throw NotFoundException("Response $reqId not found") // just ensuring it exists.
        message.resp.forEach { it.selected = false }
        req.selected = true


        val newMessages = chat.messages.slice(IntRange(0, messageIdx));
        var newChat = createChat()
        newChat.messages.addAll(newMessages)

        newChat.messages
            .forEach { mit ->
                mit.messageId = null
                mit.resp.forEach {
                    it.reqId = null
                }
            }

        newChat = chatRepository.save(newChat).awaitSingle()
        return newChat

    }

    @PreAuthorize("isAuthenticated()")
    suspend fun claimChat(chatId: String): Chat {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (chat.userId != null) throw PermissionDeniedException("Chat is already claimed")
        chat.userId = getUser()?.getUserId()
        chat = chatRepository.save(chat).awaitSingle();
        return chat;
    }

    suspend fun getChat(chatId: String): Chat {
        val chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.canView(getUser())) throw PermissionDeniedException("Can not view chat");
        return chat;
    }

    @PreAuthorize("isAuthenticated()")
    suspend fun setChatShared(chatId: String, shared: Boolean): Chat {
        var chat = chatRepository.findById(chatId).awaitSingleOrNull() ?: throw NotFoundException("Chat $chatId not found.");
        if (!chat.isOwner(getUser())) throw PermissionDeniedException("Can not modify chat");
        chat.shared = shared;
        chat = chatRepository.save(chat).awaitSingle();
        return chat;
    }

}