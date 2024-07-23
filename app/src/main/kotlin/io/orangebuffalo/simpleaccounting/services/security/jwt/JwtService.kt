package io.orangebuffalo.simpleaccounting.services.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.services.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.services.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.services.security.createTransientUserPrincipal
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class JwtService(
    private val timeService: TimeService
) {

    private val keyPair = Jwts.SIG.RS256.keyPair().build()

    fun buildJwtToken(principal: SecurityPrincipal, validTill: Instant? = null): String = Jwts.builder()
        .subject(principal.userName)
        .claim("roles", principal.roles)
        .claim("transient", principal.isTransient)
        .expiration((validTill ?: getDefaultTokenExpiration()).toDate())
        .signWith(keyPair.private)
        .compact()

    private fun getDefaultTokenExpiration() = timeService.currentTime().plus(Duration.ofMinutes(10))

    private fun Instant.toDate(): Date {
        return Date(this.toEpochMilli())
    }

    fun validateTokenAndBuildUserDetails(token: String): UserDetails {
        val jws: Jws<Claims>
        try {
            jws = Jwts.parser()
                .verifyWith(keyPair.public)
                .clock { timeService.currentTime().toDate() }
                .build()
                .parseSignedClaims(token)

        } catch (ex: JwtException) {
            throw BadCredentialsException("Bad token $token", ex)
        }

        val transient = jws.payload["transient"] as Boolean
        return if (transient) {
            createTransientUserPrincipal(jws.payload.subject, token)
        } else {
            @Suppress("UNCHECKED_CAST")
            createRegularUserPrincipal(jws.payload.subject, token, jws.payload["roles"] as List<String>)
        }
    }
}
