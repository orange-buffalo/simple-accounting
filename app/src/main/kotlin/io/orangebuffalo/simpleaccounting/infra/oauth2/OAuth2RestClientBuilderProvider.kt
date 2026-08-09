package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.security.getAuthentication
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/** Provides imperative HTTP clients authorized for the current application user. */
@Service
class OAuth2RestClientBuilderProvider(
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
) {

    fun forClient(clientRegistrationId: String): RestClient.Builder {
        val authentication = getAuthentication()
        val authorizedClient = authorizedClientManager.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                .principal(authentication)
                .build()
        ) ?: throw ClientAuthorizationRequiredException(clientRegistrationId)

        return RestClient.builder().defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "Bearer ${authorizedClient.accessToken.tokenValue}",
        )
    }
}
