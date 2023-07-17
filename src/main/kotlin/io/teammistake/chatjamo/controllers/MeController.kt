package io.teammistake.chatjamo.controllers

import io.teammistake.chatjamo.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class MeController {
    @GetMapping("/me")
    fun getUserPrincipal(
        @AuthenticationPrincipal principal: UserPrincipal?,
    ): UserPrincipal? {
        return principal
    }
}