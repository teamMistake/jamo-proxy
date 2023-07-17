package io.teammistake.chatjamo.controllers

import io.teammistake.chatjamo.dto.ChatCreationEvent
import io.teammistake.chatjamo.dto.JamoAPIError
import io.teammistake.chatjamo.dto.MessageEvent
import io.teammistake.chatjamo.service.PromptingService
import kotlinx.coroutines.reactor.asFlux
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.Exception

@RestController
@RequestMapping("/chat/{chatId}")
class PromptingController {
    @Autowired
    lateinit var promptingService: PromptingService

    data class MessageCreationRequest(
        val message: String
    )
    @PostMapping("/message")
    suspend fun createMessage(@PathVariable("chatId") chatId: String,
                              @RequestBody req: MessageCreationRequest): Flux<MessageEvent> {
        return promptingService.sendChatStreaming(chatId = chatId, req.message).asFlux()
                .onErrorResume { Mono.just(MessageEvent(JamoAPIError(it.message))) }
    }

    @PostMapping("/regenerate")
    suspend fun regenerateLast(@PathVariable("chatId") chatId: String): Flux<MessageEvent> {
        return promptingService.regenerateChatStreaming(chatId = chatId).asFlux()
                .onErrorResume { Mono.just(MessageEvent(JamoAPIError(it.message))) }
    }



    data class RatingRequest(val stars: Int)
    @PutMapping("/messages/{messageId}/{reqId}/rating")
    suspend fun rateMessage(@PathVariable("chatId") chatId: String,
                            @PathVariable("messageId") messageId: String,
                            @PathVariable("reqId") reqId: String,
                            @RequestBody request: RatingRequest) {
        promptingService.rateResponse(chatId, messageId, reqId, request.stars)
    }

    data class ChooseRequest(val chosen: String)
    @PutMapping("/messages/{messageId}/chosen")
    suspend fun chooseMssage(@PathVariable("chatId") chatId: String,
                             @PathVariable("messageId") messageId: String,
                             @RequestBody request: ChooseRequest) {
        promptingService.chooseResponse(chatId, messageId, request.chosen)
    }



}