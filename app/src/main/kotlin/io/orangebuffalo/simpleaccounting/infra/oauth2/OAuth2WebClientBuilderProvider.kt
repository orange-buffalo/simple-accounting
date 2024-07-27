package io.orangebuffalo.simpleaccounting.infra.oauth2

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * Provides a [WebClient.Builder] pre-configured to enrich a request with a token obtained for
 * the provided Client Registration ID. Whenever possible, access token renewal will be automatically
 * executed using related refresh token. In case of missing or invalid authorization,
 * [org.springframework.security.oauth2.core.OAuth2AuthorizationException] may be thrown.
 */
@Service
class OAuth2WebClientBuilderProvider(
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val authorizedClientRepository: ServerOAuth2AuthorizedClientRepository
) {

    fun forClient(clientRegistrationId: String): WebClient.Builder {
        val oauth2FilterFunction = ServerOAuth2AuthorizedClientExchangeFilterFunction(
            clientRegistrationRepository, authorizedClientRepository
        )
        oauth2FilterFunction.setDefaultClientRegistrationId(clientRegistrationId)
        return WebClient.builder().filter(oauth2FilterFunction)
    }
}
