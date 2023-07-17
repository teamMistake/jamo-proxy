package io.teammistake.chatjamo.security

import io.teammistake.chatjamo.database.User
import io.teammistake.chatjamo.database.UserRepository
import io.teammistake.chatjamo.exceptions.PermissionDeniedException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ProxyAuthenticationManager(val userRepository: UserRepository): ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        return mono {
            if (authentication is ProxyAuthenticationToken) {
                var user = userRepository.findById(authentication.user).awaitSingleOrNull();
                if (user == null) {
                    user = userRepository.save(
                        User(
                            authentication.user,
                            authentication.username,
                            0,0
                        )
                    ).awaitSingle() !!;
                }
                if (user.name != authentication.username) {
                    user.name = authentication.username
                    user = userRepository.save(user).awaitSingle() !!;
                }

                if (user.banned) throw PermissionDeniedException("Banned for reason ${user.bannedReason}")
            }

            return@mono authentication;
        }
    }
}