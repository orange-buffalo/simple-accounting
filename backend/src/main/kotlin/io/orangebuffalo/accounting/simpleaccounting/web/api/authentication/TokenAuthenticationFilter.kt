package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.security.BadTokenException
import io.orangebuffalo.accounting.simpleaccounting.services.security.JwtService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private const val BEARER = "Bearer"

class TokenAuthenticationFilter(
        private val jwtService: JwtService
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val bearerToken = extractBearerToken(exchange)
        if (bearerToken != null) {
            try {
                val userDetails = jwtService.validateTokenAndBuildUserDetails(bearerToken)

            } catch (_: BadTokenException) {
                exchange.response.statusCode = HttpStatus.BAD_REQUEST
                return Mono.empty()
            }
        }
        return chain.filter(exchange)
    }

    private fun extractBearerToken(exchange: ServerWebExchange): String? {
        return exchange.request.headers.getValuesAsList(HttpHeaders.AUTHORIZATION)
                .find { it.toLowerCase().startsWith(BEARER.toLowerCase()) }
                ?.substring(BEARER.length)
                ?.trim()
    }

}
