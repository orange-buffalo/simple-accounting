package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.DbReactiveOAuth2AuthorizedClientService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository

/**
 * Configures integration with OAuth2 providers, like Google for Google Drive storage implementation.
 * Using Spring Security OAuth2 support for authorizing this application to access user data
 * and to enrich resource servers requests with authorization tokens.
 */
@Configuration
class Oauth2Config {

    @Bean
    fun authorizedClientService(
        repository: PersistentOAuth2AuthorizedClientRepository,
        clientRegistrationRepository: ReactiveClientRegistrationRepository
    ): ReactiveOAuth2AuthorizedClientService =
        DbReactiveOAuth2AuthorizedClientService(repository, clientRegistrationRepository)

    @Bean
    fun accessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        WebClientReactiveAuthorizationCodeTokenResponseClient()

}
