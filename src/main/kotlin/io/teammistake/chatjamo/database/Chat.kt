package io.teammistake.chatjamo.database

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

@Document("chat")
class Chat(
    @MongoId
    var chatId: String,
    var userId: String?,
    var shared: Boolean,
    var messages: MutableList<ChatMessage>,
    var title: String = "",
    var creationTimestamp: Instant,
    var generating: Boolean = false
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
    var type: ResponseType
)

open class Experiment()
enum class ResponseType {
    PLAIN, REGENERATED
}
