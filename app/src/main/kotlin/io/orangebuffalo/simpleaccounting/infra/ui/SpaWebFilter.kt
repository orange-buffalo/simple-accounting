package io.orangebuffalo.simpleaccounting.infra.ui

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

open class SpaWebFilter : WebFilter {

    private val requestMatcher = AndServerWebExchangeMatcher(
        ServerWebExchangeMatchers.pathMatchers("/**"),
        NegatedServerWebExchangeMatcher(EndpointRequest.toAnyEndpoint()),
        NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/favicon.ico")),
        NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**")),
        NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/assets/**"))
    )

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return requestMatcher.matches(exchange)
            .map { it.isMatch }
            .flatMap { matches ->
                if (matches) {
                    chain.filter(exchange.mutate().request(
                        exchange.request.mutate().path("/index.html").build()
                    ).build())
                } else {
                    chain.filter(exchange)
                }
            }
    }
}
