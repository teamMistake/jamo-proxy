package io.teammistake.chatjamo.security

import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class ProxyAuthenticationConverter: ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange?): Mono<Authentication> {
        return mono {
            val user = exchange?.request?.headers?.getFirst("X-Forwarded-User") ?: return@mono null;
            val username = exchange.request.headers.getFirst("X-Forwarded-Preferred-Username") ?: throw IllegalArgumentException("preferred username null");
            val email = exchange.request.headers.getFirst("X-Forwarded-Email")


            ProxyAuthenticationToken(user, username, email)
        }
    }
}