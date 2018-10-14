package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.security.core.ReactiveAuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class UserNamePasswordAuthenticationProvider(
        private val platformUserService: PlatformUserService,
        private val passwordEncoder: PasswordEncoder
) : ReactiveAuthenticationProvider {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return platformUserService.getUserByUserName(authentication.name)
                .publishOn(Schedulers.parallel())
                .filter { passwordEncoder.matches(authentication.credentials as String, it.passwordHash) }
                .switchIfEmpty(Mono.defer {
                    Mono.error<PlatformUser>(BadCredentialsException("Invalid Credentials"))
                })
                .map<Authentication> {
                    UsernamePasswordAuthenticationToken(
                            User.builder()
                                    .username(it.userName)
                                    .password(it.passwordHash)
                                    .accountExpired(false)
                                    .accountLocked(false)
                                    .credentialsExpired(false)
                                    .roles(if (it.isAdmin) "ADMIN" else "USER")
                                    .build(),
                            it.passwordHash)
                }
    }

    override fun supports(authenticationClass: KClass<out Any>): Boolean {
        return authenticationClass.isSubclassOf(UsernamePasswordAuthenticationToken::class)
    }
}