package io.orangebuffalo.accounting.simpleaccounting.services.security.jwt

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails

class JwtAuthenticationToken(
        val token: String,
        val user: UserDetails? = null
) : AbstractAuthenticationToken(user?.authorities) {

    init {
        isAuthenticated = user != null
    }

    override fun getCredentials(): Any {
        return token
    }

    override fun getPrincipal(): Any {
        return user ?: token
    }
}