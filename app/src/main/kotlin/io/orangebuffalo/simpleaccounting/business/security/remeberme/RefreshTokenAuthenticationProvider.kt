package io.orangebuffalo.simpleaccounting.business.security.remeberme

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class RefreshTokenAuthenticationProvider(
    private val refreshTokensService: RefreshTokensService
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? {
        if (authentication !is RefreshAuthenticationToken) return null
        val token = authentication.credentials as String
        return RefreshAuthenticationToken(
            token,
            refreshTokensService.validateTokenAndBuildUserDetails(token)
        )
    }

    override fun supports(authentication: Class<*>): Boolean =
        RefreshAuthenticationToken::class.java.isAssignableFrom(authentication)
}
