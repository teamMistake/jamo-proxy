package io.teammistake.chatjamo.database

import org.springframework.data.mongodb.core.mapping.Document

@Document("user")
class User(
    val userId: String,
    var name: String,
    var sentMessages: Long,
    var receivedMessages: Long,
    var banned: Boolean = false,
    var bannedReason: String? = null
)