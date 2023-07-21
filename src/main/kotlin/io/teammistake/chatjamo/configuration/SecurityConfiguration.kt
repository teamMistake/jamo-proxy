package io.teammistake.chatjamo.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import org.springframework.web.server.handler.ExceptionHandlingWebHandler
import reactor.core.publisher.Mono
import java.util.function.Consumer
import java.util.function.Function


@EnableWebFluxSecurity
@Configuration
@EnableReactiveMethodSecurity(useAuthorizationManager = false)
class SecurityConfiguration {

    @Bean
    fun configure(http: ServerHttpSecurity, jwtAuthenticationWebFilter: AuthenticationWebFilter, handlers: List<WebExceptionHandler>): SecurityWebFilterChain? {
        val generalHandler = {
                exchange: ServerWebExchange, err: Throwable ->
            var completion = Mono.error<Void>(err);

            for (handler in handlers) {
                completion = completion
                    .doOnError { error: Throwable? ->
                        exchange.getAttributes().put(ExceptionHandlingWebHandler.HANDLED_WEB_EXCEPTION, error)
                    }
                    .onErrorResume { ex: Throwable? ->
                        handler.handle(
                            exchange,
                            ex!!
                        )
                    }
            }

            completion
        }
        return http
            .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling {
                it.accessDeniedHandler(generalHandler)
                    .authenticationEntryPoint(generalHandler)
            }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .csrf {
                it.disable()
            }
            .build()
    }
}