package io.orangebuffalo.accounting.simpleaccounting.services.security.core

import org.springframework.security.core.Authentication
import kotlin.reflect.KClass

interface AuthenticationProvider {

    /**
     * Performs authentication with the same contract as [ReactiveAuthenticationProvider#authenticate]
     */
    suspend fun authenticate(authentication: Authentication) : Authentication?

    fun supports(authenticationClass: KClass<out Any>): Boolean

}