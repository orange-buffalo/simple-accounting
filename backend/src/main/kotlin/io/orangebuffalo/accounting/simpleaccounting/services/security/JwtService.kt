package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
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

}