package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.DbReactiveOAuth2AuthorizedClientService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import reactor.core.publisher.Mono

/**
 * Configures integration with OAuth2 providers, like Google for Google Drive storage implementation.
 * Using Spring Security OAuth2 support for authorizing this application to access user data
 * and to enrich resource servers requests with authorization tokens.
 */
@Configuration
class Oauth2Config {

    @Bean
    fun reactiveClientRegistrationRepository(
        clientRegistrationRepository: ClientRegistrationRepository,
    ): ReactiveClientRegistrationRepository = ReactiveClientRegistrationRepository { registrationId ->
        Mono.justOrEmpty(clientRegistrationRepository.findByRegistrationId(registrationId))
    }

    @Bean
    fun reactiveAuthorizedClientService(
        repository: PersistentOAuth2AuthorizedClientRepository,
        clientRegistrationRepository: ReactiveClientRegistrationRepository
    ): ReactiveOAuth2AuthorizedClientService =
        DbReactiveOAuth2AuthorizedClientService(repository, clientRegistrationRepository)

    @Bean
    fun reactiveAuthorizedClientRepository(
        authorizedClientService: ReactiveOAuth2AuthorizedClientService,
    ): ServerOAuth2AuthorizedClientRepository =
        AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService)

    @Bean
    fun reactiveAccessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        WebClientReactiveAuthorizationCodeTokenResponseClient()

}
