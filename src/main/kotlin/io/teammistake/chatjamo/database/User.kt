package io.teammistake.chatjamo.database

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("user")
class User(
    @MongoId
    val userId: String,
    var name: String,
    var sentMessages: Long,
    var receivedMessages: Long,
    var banned: Boolean = false,
    var bannedReason: String? = null
)