package io.teammistake.chatjamo.database

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Exp
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

@Document("chat")
class Chat(
    @MongoId
    var chatId: String,
    @Indexed
    var userId: String?,
    var shared: Boolean = false,
    var messages: MutableList<ChatMessage> = mutableListOf(),
    var title: String = "",
    var creationTimestamp: Instant,
    var generating: Boolean = false
)

@Document("chat")
data class LightChat(
    @MongoId
    val chatId: String,
    val title: String,
    val creationTimestamp: Instant
)

class ChatMessage(
    var messageId: String?, // null if it does not belong to current chat
    var req: String,
    var resp: MutableList<ChatMessageResponse>,
    var experiment: Experiment
);

class ChatMessageResponse(
    var reqId: String?, // null if it does not belong to current chat
    var text: String,
    var model: String,
    var hyperParameter: MutableMap<String, String>,
    var selected: Boolean,
    var feedback: Int?,
    var error: String? = null,
    var type: ResponseType
)

@JsonTypeInfo(use= JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
open class Experiment()
class Normal() : Experiment()
class SameModel() : Experiment()
class Multiple(val metadata: String = "I don't think you care about this experiment") : Experiment()
enum class ResponseType {
    PLAIN, REGENERATED
}
