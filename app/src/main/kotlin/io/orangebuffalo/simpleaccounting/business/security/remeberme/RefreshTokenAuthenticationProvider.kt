package io.orangebuffalo.simpleaccounting.business.security.remeberme

import io.orangebuffalo.simpleaccounting.business.security.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class RefreshTokenAuthenticationProvider(
    private val refreshTokensService: RefreshTokensService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = authentication
        .mono<RefreshAuthenticationToken> { refreshAuthenticationToken ->
            val token = refreshAuthenticationToken.credentials as String
            RefreshAuthenticationToken(
                token,
                refreshTokensService.validateTokenAndBuildUserDetails(token)
            )
        }
}
