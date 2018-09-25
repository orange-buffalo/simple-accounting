package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtAuthenticationToken
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private const val BEARER = "Bearer"

class TokenAuthenticationFilter(
        private val authenticationManager: ReactiveAuthenticationManager
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return extractBearerToken(exchange)
                .flatMap { authenticationManager.authenticate(JwtAuthenticationToken(it)) }
                .doOnNext { SecurityContextHolder.getContext().authentication = it }
                .flatMap { chain.filter(exchange) }
                .switchIfEmpty(Mono.defer { chain.filter(exchange) })
    }

    private fun extractBearerToken(exchange: ServerWebExchange): Mono<String> {
        return Flux.fromIterable(exchange.request.headers.getValuesAsList(HttpHeaders.AUTHORIZATION))
                .filter { it.toLowerCase().startsWith(BEARER.toLowerCase()) }
                .map { it.substring(BEARER.length).trim() }
                .filter { it.isNotEmpty() }
                .next()
    }
}
