package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.security.core.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class UserNamePasswordAuthenticationProvider(
    private val platformUserService: PlatformUserService,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationProvider {

    override suspend fun authenticate(authentication: Authentication): Authentication? {
        return platformUserService.getUserByUserName(authentication.name)
            ?.also { user ->
                if (!passwordEncoder.matches(authentication.credentials as String, user.passwordHash)) {
                    throw BadCredentialsException("Invalid Credentials")
                }
            }
            ?.let { user ->
                UsernamePasswordAuthenticationToken(
                    User.builder()
                        .username(user.userName)
                        .password(user.passwordHash)
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .roles(if (user.isAdmin) "ADMIN" else "USER")
                        .build(),
                    user.passwordHash
                )
            }
            ?: throw BadCredentialsException("Invalid Credentials")
    }

    override fun supports(authenticationClass: KClass<out Any>): Boolean {
        return authenticationClass.isSubclassOf(UsernamePasswordAuthenticationToken::class)
    }
}