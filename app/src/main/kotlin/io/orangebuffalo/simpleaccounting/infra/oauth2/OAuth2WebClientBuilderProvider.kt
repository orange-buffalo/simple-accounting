package io.orangebuffalo.simpleaccounting.infra.oauth2

import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Provides a [WebClient.Builder] pre-configured to enrich a request with a token obtained for
 * the provided Client Registration ID. Whenever possible, access token renewal will be automatically
 * executed using related refresh token. In case of missing or invalid authorization,
 * [org.springframework.security.oauth2.core.OAuth2AuthorizationException] may be thrown.
 */
@Service
class OAuth2WebClientBuilderProvider(
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val authorizedClientService: ReactiveOAuth2AuthorizedClientService,
) {

    fun forClient(clientRegistrationId: String): WebClient.Builder {
        val authorizedClientServiceManager = AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService,
        )
        authorizedClientServiceManager.setAuthorizedClientProvider(
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .refreshToken()
                .build()
        )
        val authorizedClientManager = ReactiveOAuth2AuthorizedClientManager { request ->
            authorizedClientServiceManager.authorize(request)
                .switchIfEmpty(Mono.error(ClientAuthorizationRequiredException(clientRegistrationId)))
        }
        val oauth2FilterFunction = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
        oauth2FilterFunction.setDefaultClientRegistrationId(clientRegistrationId)
        return WebClient.builder().filter(oauth2FilterFunction)
    }
}
