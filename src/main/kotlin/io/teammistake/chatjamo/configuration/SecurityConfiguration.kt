package io.teammistake.chatjamo.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono


@EnableWebFluxSecurity
@Configuration
class SecurityConfiguration {

    @Value("#{environment.DEX_HOST}")
    lateinit var dex: String;

    @Value("#{environment.CLIENT_ID}")
    lateinit var clientId: String;

    @Value("#{environment.CLIENT_SECRET}")
    lateinit var clientSecret: String;

    @Bean
    @Throws(Exception::class)
    fun configure(http: ServerHttpSecurity): SecurityWebFilterChain? {
        val clientRegistration: ClientRegistration = ClientRegistration
            .withRegistrationId("dex")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .issuerUri(dex).build();
        return http
            .oauth2Login {
                it.clientRegistrationRepository { name ->
                    if (name == "dex") Mono.just(clientRegistration)
                    Mono.empty()
                }
            }.build()
    }
}