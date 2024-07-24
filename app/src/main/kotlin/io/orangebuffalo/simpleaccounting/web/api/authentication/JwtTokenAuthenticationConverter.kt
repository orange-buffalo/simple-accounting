package io.orangebuffalo.simpleaccounting.web.api.authentication

import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtAuthenticationToken
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private const val BEARER = "Bearer"

class JwtTokenAuthenticationConverter : ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        return extractBearerToken(exchange)
            .map { JwtAuthenticationToken(it) }
    }

    private fun extractBearerToken(exchange: ServerWebExchange): Mono<String> {
        return Flux.fromIterable(exchange.request.headers.getValuesAsList(HttpHeaders.AUTHORIZATION))
            .filter { it.lowercase().startsWith(BEARER.lowercase()) }
            .map { it.substring(BEARER.length).trim() }
            .filter { it.isNotEmpty() }
            .next()
    }
}
