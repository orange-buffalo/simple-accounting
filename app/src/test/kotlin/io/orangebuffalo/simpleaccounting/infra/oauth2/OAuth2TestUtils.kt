package io.orangebuffalo.simpleaccounting.infra.oauth2

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.mockito.internal.util.MockUtil
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Mocks an authorization failure during the OAuth2 flow.
 */
fun OAuth2WebClientBuilderProvider.mockAuthorizationFailure() {
    check(MockUtil.isMock(this)) { "Can only stub mocks" }

    whenever(this.forClient(any())) doReturn WebClient.builder()
        .filter(ExchangeFilterFunction.ofRequestProcessor {
            Mono.error { ClientAuthorizationRequiredException("stub client") }
        })
}

/**
 * Mocks successful authorization state and sets the provided token as a Bearer authorization.
 */
fun OAuth2WebClientBuilderProvider.mockAccessToken(token: String) {
    check(MockUtil.isMock(this)) { "Can only stub mocks" }

    whenever(this.forClient(any())) doReturn WebClient.builder()
        .filter(ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
            Mono.just(clientRequest)
                .map {
                    ClientRequest.from(clientRequest)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                        .build()
                }
        })
}

