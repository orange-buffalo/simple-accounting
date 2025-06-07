package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.common.Strings.normalisedLevenshteinDistance
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.nimbusds.jwt.JWTParser
import mu.KotlinLogging
import wiremock.com.fasterxml.jackson.annotation.JsonProperty
import java.text.ParseException

private val log = KotlinLogging.logger { }

/**
 * mockOAuthServer is very hard to configure (if possible at all) to generate stable
 * JWT tokens that could be used for mocking and verification. Often the signature does
 * not match even though the claims are equal, which indicates they use inconsistent
 * data for signing.
 * For this reason, we use a custom matcher to verify the JWT ID (jti) claim,
 * which is configured in the test code via [OAuthMocksProvider.token] method. The rest
 * of the JWT claims are not verified, we consider this internal implementation detail
 * of the mock server.
 */
internal class JwtIdMatcher(
    @param:JsonProperty("jwtId") private val expectedJwtId: String,
) : StringValuePattern("JWT token having jti=$expectedJwtId") {
    override fun match(value: String?): MatchResult {
        if (value == null) {
            return MatchResult.noMatch()
        }
        if (!value.startsWith("Bearer ")) {
            log.warn { "Provided header is not a Bearer token" }
            // not a Bearer token
            return MatchResult.noMatch()
        }

        val jwtString = value.substring(7)
        try {
            val jwt = JWTParser.parse(jwtString)
            val jti = jwt.jwtClaimsSet.jwtid
            log.debug { "Matching request. Actual JWT ID is $jti" }
            return object : MatchResult() {
                override fun isExactMatch(): Boolean = jti == expectedJwtId
                override fun getDistance(): Double = normalisedLevenshteinDistance(expectedValue, value)
            }
        } catch (e: ParseException) {
            log.warn { "Provided header is not a valid JWT" }
            // not a JWT
            return MatchResult.noMatch()
        }
    }
}
