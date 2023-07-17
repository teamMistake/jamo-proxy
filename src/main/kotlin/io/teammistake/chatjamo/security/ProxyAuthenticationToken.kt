package io.teammistake.chatjamo.security

import org.springframework.security.authentication.AbstractAuthenticationToken

class ProxyAuthenticationToken(val user: String, val username: String, val email: String?): AbstractAuthenticationToken(listOf()) {
    init {
        isAuthenticated = true
    }


    private val userPrincipal = UserPrincipal(user, username, email)

    override fun getCredentials(): String {
        return "";
    }
    override fun getPrincipal(): UserPrincipal {
        return userPrincipal
    }
}