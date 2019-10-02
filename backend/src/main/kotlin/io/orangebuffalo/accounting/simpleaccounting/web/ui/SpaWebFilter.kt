package io.orangebuffalo.accounting.simpleaccounting.web.ui

import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class SpaWebFilter : WebFilter {

    private val requestMatcher = AndServerWebExchangeMatcher(
        ServerWebExchangeMatchers.pathMatchers("/**"),
        NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**")),
        NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/static/**"))
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