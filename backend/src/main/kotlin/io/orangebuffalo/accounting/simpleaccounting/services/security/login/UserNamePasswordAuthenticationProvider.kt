package io.orangebuffalo.accounting.simpleaccounting.services.security.login

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.security.mono
import io.orangebuffalo.accounting.simpleaccounting.services.security.toSecurityPrincipal
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class UserNamePasswordAuthenticationProvider(
    private val platformUserService: PlatformUserService,
    private val passwordEncoder: PasswordEncoder
) : ReactiveAuthenticationManager {


    override fun authenticate(authentication: Authentication): Mono<Authentication> = authentication
        .mono<UsernamePasswordAuthenticationToken> { usernamePasswordToken ->
            platformUserService.getUserByUserName(usernamePasswordToken.name)
                ?.also { user ->
                    if (!passwordEncoder.matches(usernamePasswordToken.credentials as String, user.passwordHash)) {
                        throw BadCredentialsException("Invalid Credentials")
                    }
                }
                ?.let { user ->
                    UsernamePasswordAuthenticationToken(
                        user.toSecurityPrincipal(),
                        user.passwordHash
                    )
                }
                ?: throw BadCredentialsException("Invalid Credentials")
        }
}
