package io.orangebuffalo.accounting.simpleaccounting.services.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder

suspend fun ensureRegularUserPrincipal(): SecurityPrincipal = getCurrentPrincipal()
    .apply { if (isTransient) throw InsufficientUserType() }

suspend fun getAuthenticationOrNull(): Authentication? {
    return ReactiveSecurityContextHolder.getContext()
    .map { it.authentication }
        .awaitFirstOrNull()
}

suspend fun getAuthentication(): Authentication {
    return getAuthenticationOrNull()
        ?: throw IllegalStateException("Authentication is not set")
}

suspend fun getCurrentPrincipal(): SecurityPrincipal {
    val authentication = getAuthentication()
    return authentication.principal as SecurityPrincipal
}