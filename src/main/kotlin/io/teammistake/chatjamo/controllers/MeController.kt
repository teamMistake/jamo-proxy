package io.teammistake.chatjamo.controllers

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class MeController {
    @GetMapping("/me")

    fun getOidcUserPrincipal(
        @AuthenticationPrincipal principal: OidcUser?,
    ): OidcUser? {
        return principal
    }
}