package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class JwtService {

    private val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)

    fun buildJwtToken(userDetails: UserDetails): String {
        return Jwts.builder()
                .setSubject(userDetails.username)
                .claim("authorities", userDetails.authorities.map { it.authority })
                .signWith(keyPair.private)
                .compact()
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

        return User.builder()
                .username(jws.body.subject)
                .password(token)
                .authorities(*(jws.body["authorities"] as List<String>).toTypedArray())
                .build()
    }

}