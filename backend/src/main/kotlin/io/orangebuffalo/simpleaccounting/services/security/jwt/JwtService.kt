package io.orangebuffalo.simpleaccounting.services.security.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.orangebuffalo.simpleaccounting.services.business.TimeService
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

    private val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)

    fun buildJwtToken(principal: SecurityPrincipal, validTill: Instant? = null): String = Jwts.builder()
        .setSubject(principal.userName)
        .claim("roles", principal.roles)
        .claim("transient", principal.isTransient)
        .setExpiration((validTill ?: getDefaultTokenExpiration()).toDate())
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
                .setSigningKey(keyPair.public)
                .parseClaimsJws(token)

        } catch (ex: JwtException) {
            throw BadCredentialsException("Bad token $token", ex)
        }

        val transient = jws.body["transient"] as Boolean
        return if (transient) {
            createTransientUserPrincipal(jws.body.subject, token)
        } else {
            @Suppress("UNCHECKED_CAST")
            createRegularUserPrincipal(jws.body.subject, token, jws.body["roles"] as List<String>)
        }
    }
}
