package io.orangebuffalo.accounting.simpleaccounting.services.security.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ProviderNotFoundException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class DelegatingReactiveAuthenticationManager(
    private val providers: List<AuthenticationProvider>
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = GlobalScope.mono {
        val authenticationProvider = providers
            .firstOrNull { it.supports(authentication::class) }
            ?: throw ProviderNotFoundException(
                "Cannot find authentication provider to consume ${authentication::class}"
            )

        authenticationProvider.authenticate(authentication)
    }
}