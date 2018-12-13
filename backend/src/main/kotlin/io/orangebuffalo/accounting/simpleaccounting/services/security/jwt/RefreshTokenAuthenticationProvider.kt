package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import io.orangebuffalo.accounting.simpleaccounting.services.security.core.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class RefreshTokenAuthenticationProvider(
    private val refreshTokenService: RefreshTokenService
) : AuthenticationProvider {

    override suspend fun authenticate(authentication: Authentication): Authentication? {
        val token = authentication.credentials as String
        return RefreshAuthenticationToken(
            token,
            refreshTokenService.validateTokenAndBuildUserDetails(token)
        )
    }

    override fun supports(authenticationClass: KClass<out Any>): Boolean {
        return authenticationClass.isSubclassOf(RefreshAuthenticationToken::class)
    }
}