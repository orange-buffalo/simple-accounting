package io.orangebuffalo.simpleaccounting.services.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.AbstractAuthenticationToken
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

suspend fun getCurrentPrincipalOrNull(): SecurityPrincipal? {
    val authentication = getAuthenticationOrNull()
    return authentication?.principal as SecurityPrincipal?
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

suspend fun <T : Any?> runAs(principal: SpringSecurityPrincipal, block: suspend () -> T): T {
    return mono {
        block()
    }.contextWrite(
        ReactiveSecurityContextHolder.withAuthentication(ProgrammaticAuthentication(principal))
    ).awaitSingle()
}

class ProgrammaticAuthentication(val user: SpringSecurityPrincipal) : AbstractAuthenticationToken(user.authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials() = null
    override fun getPrincipal() = user
}
