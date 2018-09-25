package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import io.orangebuffalo.accounting.simpleaccounting.services.security.core.ReactiveAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class JwtAuthenticationProvider(
        private val jwtService: JwtService
) : ReactiveAuthenticationProvider {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return Mono.just(authentication)
                .map { it.credentials as String }
                .map(jwtService::validateTokenAndBuildUserDetails)
                .map<Authentication> {
                    JwtAuthenticationToken(it.password, it)
                }
    }

    override fun supports(authenticationClass: KClass<out Any>): Boolean {
        return authenticationClass.isSubclassOf(JwtAuthenticationToken::class)
    }
}