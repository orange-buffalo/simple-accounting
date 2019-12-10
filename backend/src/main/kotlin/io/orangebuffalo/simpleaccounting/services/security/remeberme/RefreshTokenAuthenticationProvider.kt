package io.orangebuffalo.simpleaccounting.services.security.remeberme

import io.orangebuffalo.simpleaccounting.services.security.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class RefreshTokenAuthenticationProvider(
    private val refreshTokenService: RefreshTokenService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = authentication
        .mono<RefreshAuthenticationToken> { refreshAuthenticationToken ->
            val token = refreshAuthenticationToken.credentials as String
            RefreshAuthenticationToken(
                token,
                refreshTokenService.validateTokenAndBuildUserDetails(token)
            )
        }
}
