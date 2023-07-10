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
    var history: List<ChatHistoryPart>,
    var title: String = "",
    var creationTimestamp: Instant
)

class ChatHistoryPart(
    var req: String,
    var resp: List<ChatHistoryResponse>
);

class ChatHistoryResponse(
    var reqId: String,
    var text: String,
    var model: String,
    var hyperParameter: Map<String, String>,
    var selected: Boolean
)
