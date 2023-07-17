package io.teammistake.chatjamo.security

data class UserPrincipal(
    val user: String, val preferredUsernme: String?, val email: String?
)
