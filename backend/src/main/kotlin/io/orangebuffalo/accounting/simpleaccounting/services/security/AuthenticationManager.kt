package io.orangebuffalo.accounting.simpleaccounting.services.security

import org.springframework.security.authentication.ProviderNotFoundException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AuthenticationManager(
        private val jpaUserDetailsService: JpaUserDetailsService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return Flux.fromIterable(listOf(jpaUserDetailsService))
                .filter { it.supports(authentication::class) }
                .flatMap { it.authenticate(authentication) }
                .next()
                .switchIfEmpty(Mono.error(ProviderNotFoundException(
                        "Cannot find authentication provider to consume ${authentication::class}")))
    }
}