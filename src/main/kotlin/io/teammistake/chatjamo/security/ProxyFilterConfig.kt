package io.teammistake.chatjamo.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono


@Configuration
class ProxyFilterConfig {
    @Autowired
    lateinit var jwtReactiveAuthenticationManager: ProxyAuthenticationManager;
    @Autowired
    lateinit var proxyAuthenticationConverter: ProxyAuthenticationConverter;


    @Bean
    fun jwtAuthenticationWebFilter(): AuthenticationWebFilter? {
        val filter = AuthenticationWebFilter(jwtReactiveAuthenticationManager)
        filter.setServerAuthenticationConverter(proxyAuthenticationConverter)
        filter.setAuthenticationSuccessHandler(WebFilterChainServerAuthenticationSuccessHandler())
        filter.setAuthenticationFailureHandler { exchange: WebFilterExchange?, exception: AuthenticationException? ->
            Mono.error(exception ?: BadCredentialsException("?"))
        }
        return filter
    }
}