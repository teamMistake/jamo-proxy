package io.teammistake.chatjamo.database

import org.springframework.data.mongodb.core.mapping.Document

@Document("user")
class User(
    val userId: String,
    val sentMessages: Long,
    val receivedMessages: Long,
    val banned: Boolean,
    val bannedReason: String
)