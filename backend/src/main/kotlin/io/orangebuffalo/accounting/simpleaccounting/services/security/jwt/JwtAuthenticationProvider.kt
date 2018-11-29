package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import io.orangebuffalo.accounting.simpleaccounting.services.security.core.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class JwtAuthenticationProvider(
    private val jwtService: JwtService
) : AuthenticationProvider {

    override suspend fun authenticate(authentication: Authentication): Authentication? {
        return authentication
            .let { it.credentials as String }
            .let { jwtService.validateTokenAndBuildUserDetails(it) }
            .let { JwtAuthenticationToken(it.password, it) }
    }

    override fun supports(authenticationClass: KClass<out Any>): Boolean {
        return authenticationClass.isSubclassOf(JwtAuthenticationToken::class)
    }
}