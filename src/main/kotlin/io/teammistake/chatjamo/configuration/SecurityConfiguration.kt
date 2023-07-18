package io.teammistake.chatjamo.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono


@EnableWebFluxSecurity
@Configuration
@EnableReactiveMethodSecurity(useAuthorizationManager = false)
class SecurityConfiguration {

    @Bean
    fun configure(http: ServerHttpSecurity, jwtAuthenticationWebFilter: AuthenticationWebFilter): SecurityWebFilterChain? {
        return http
            .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .csrf {
                it.disable()
            }
            .build()
    }
}