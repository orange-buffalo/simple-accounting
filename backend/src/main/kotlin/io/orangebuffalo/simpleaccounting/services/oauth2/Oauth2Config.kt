package io.orangebuffalo.simpleaccounting.services.oauth2

import io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2.PersistentOAuth2AuthorizedClientRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository

@Configuration
class Oauth2Config {

    @Bean
    fun reactiveOAuth2AuthorizedClientService(
        repository: PersistentOAuth2AuthorizedClientRepository,
        clientRegistrationRepository: ReactiveClientRegistrationRepository
    ): ReactiveOAuth2AuthorizedClientService =
        DbReactiveOAuth2AuthorizedClientService(repository, clientRegistrationRepository)

    @Bean
    fun accessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        WebClientReactiveAuthorizationCodeTokenResponseClient()

}
