package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import io.orangebuffalo.accounting.simpleaccounting.services.security.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationProvider(
    private val jwtService: JwtService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = authentication
        .mono<JwtAuthenticationToken> { jwtAuthenticationToken ->
            jwtAuthenticationToken
                .let { it.credentials as String }
                .let { jwtService.validateTokenAndBuildUserDetails(it) }
                .let { JwtAuthenticationToken(it.password, it) }
        }
}
