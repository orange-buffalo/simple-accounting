package io.orangebuffalo.simpleaccounting.services.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

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

inline fun <reified T : Authentication> Authentication.mono(
    crossinline block: suspend (T) -> Authentication
): Mono<Authentication> {
    return if (this is T) {
        val t = this
        kotlinx.coroutines.reactor.mono {
            block(t)
        }
    } else {
        Mono.empty()
    }
}
