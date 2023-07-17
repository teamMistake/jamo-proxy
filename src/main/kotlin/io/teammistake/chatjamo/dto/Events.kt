package io.teammistake.chatjamo.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.teammistake.chatjamo.database.Chat
import io.teammistake.chatjamo.database.ChatMessage

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(name="lm_response", value=ResponseGenerationEvent::class),
    JsonSubTypes.Type(name="chat", value=ChatCreationEvent::class),
    JsonSubTypes.Type(name="message", value=MessageCreationEvent::class),
    JsonSubTypes.Type(name="lm_reqids", value=ResponseIdsEvent::class),
    JsonSubTypes.Type(name="error", value=JamoAPIError::class),
    JsonSubTypes.Type(name="lm_error", value=ResponseGenerationError::class)
)
open class MessageEventData;

data class MessageEvent(
    val data: MessageEventData
)


data class ChatCreationEvent(
    val chat: Chat
): MessageEventData();

data class MessageCreationEvent(
    val chatId: String,
    val message: ChatMessage
): MessageEventData()

data class ResponseIdsEvent(
    val reqIds: List<APIResponseHeader>
): MessageEventData()

data class ResponseGenerationEvent(
    val chatId: String,
    val messageId: String,
    val reqId: String,
    val data: InferenceResponse
): MessageEventData()