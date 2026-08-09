package io.orangebuffalo.simpleaccounting.business.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

fun ensureRegularUserPrincipal(): SecurityPrincipal = getCurrentPrincipal()
    .apply { if (isTransient) throw InsufficientUserType() }

fun getAuthenticationOrNull(): Authentication? = SecurityContextHolder.getContext().authentication

fun getAuthentication(): Authentication {
    return getAuthenticationOrNull()
        ?: throw IllegalStateException("Authentication is not set")
}

fun getCurrentPrincipal(): SecurityPrincipal {
    val authentication = getAuthentication()
    return authentication.principal as SecurityPrincipal
}

fun getCurrentPrincipalOrNull(): SecurityPrincipal? {
    val authentication = getAuthenticationOrNull()
    return authentication?.principal as SecurityPrincipal?
}

inline fun <T> runAs(principal: SpringSecurityPrincipal, block: () -> T): T {
    return runWithAuthentication(ProgrammaticAuthentication(principal), block)
}

inline fun <T> runWithAuthentication(authentication: Authentication?, block: () -> T): T {
    val previousContext = SecurityContextHolder.getContext()
    val context = SecurityContextHolder.createEmptyContext().apply {
        this.authentication = authentication
    }
    SecurityContextHolder.setContext(context)
    return try {
        block()
    } finally {
        SecurityContextHolder.setContext(previousContext)
    }
}

class ProgrammaticAuthentication(val user: SpringSecurityPrincipal) : AbstractAuthenticationToken(user.authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials() = null
    override fun getPrincipal() = user
}
