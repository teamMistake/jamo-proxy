package io.teammistake.chatjamo.database

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
    var shared: Boolean,
    var messages: MutableList<ChatMessage>,
    var title: String = "",
    var creationTimestamp: Instant,
    var generating: Boolean = false
)

data class LightChat(
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

open class Experiment()
class Normal() : Experiment()
enum class ResponseType {
    PLAIN, REGENERATED
}
