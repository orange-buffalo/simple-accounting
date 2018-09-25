package io.orangebuffalo.accounting.simpleaccounting.services.security.core

import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

interface ReactiveAuthenticationProvider {

    /**
     * Performs authentication with the same contract as [ReactiveAuthenticationProvider#authenticate]
     */
    fun authenticate(authentication: Authentication) : Mono<Authentication>

    fun supports(authenticationClass: KClass<out Any>): Boolean

}