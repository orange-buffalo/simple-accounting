package io.orangebuffalo.accounting.simpleaccounting.web

import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.core.publisher.onErrorResume

class ErrorHandlerFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return chain.filter(exchange)
                .onErrorResume(AuthenticationException::class) {
                    exchange.response.statusCode = HttpStatus.FORBIDDEN
                    Mono.empty()
                }
    }
}
