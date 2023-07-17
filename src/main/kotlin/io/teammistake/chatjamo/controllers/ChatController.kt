package io.teammistake.chatjamo.controllers

import io.teammistake.chatjamo.database.Chat
import io.teammistake.chatjamo.dto.ChatCreationEvent
import io.teammistake.chatjamo.dto.MessageEvent
import io.teammistake.chatjamo.service.ChatService
import io.teammistake.chatjamo.service.PromptingService
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/chat")
class ChatController {

    data class CreateChatRequest(
        val initialPrompt: String
    )

    @Autowired
    lateinit var chatService: ChatService;
    @Autowired
    lateinit var promptingService: PromptingService;

    @PostMapping("/create",  produces = [MediaType.APPLICATION_NDJSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun create(@RequestBody request: CreateChatRequest): Flux<MessageEvent> {
        val chat = chatService.createChat();

        return Flux.concat(
            Mono.just(MessageEvent(ChatCreationEvent(chat))),
            flow {
                promptingService.sendChatStreaming(chatId = chat.chatId, request.initialPrompt)
                    .collect {
                        emit(it)
                    }
            }.asFlux()
        )
    }

    data class ShareRequest(
        val share: Boolean
    )
    @PutMapping("/{chatId}/share")
    @PreAuthorize("isAuthenticated()")
    suspend fun share(@PathVariable("chatId") id: String,
        @RequestBody request: ShareRequest) {
        chatService.setChatShared(id, request.share)
    }

    @GetMapping("/{chatId}")
    suspend fun get(@PathVariable("chatId") id: String): Chat {
        return chatService.getChat(id);
    }

    @PostMapping("/{chatId}/claim")
    @PreAuthorize("isAuthenticated()")
    suspend fun claim(@PathVariable("chatId") id: String) {
        chatService.claimChat(id);
    }

    data class DeriveRequest(
        val messageId: String,
        val requestId: String
    )

    @PostMapping("/{chatId}/derive")
    @PreAuthorize("isAuthenticated()")
    suspend fun derive(@PathVariable("chatId") id: String,
                       @RequestBody request: DeriveRequest): Chat {
        return chatService.deriveChatFrom(id, request.messageId, request.requestId)
    }
}