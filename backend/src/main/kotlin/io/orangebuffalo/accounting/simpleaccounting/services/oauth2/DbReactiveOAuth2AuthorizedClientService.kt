package io.orangebuffalo.accounting.simpleaccounting.services.oauth2

import io.orangebuffalo.accounting.simpleaccounting.services.integration.voidMono
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.oauth2.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.oauth2.PersistentOAuth2AuthorizedClientRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import reactor.core.publisher.Mono

class DbReactiveOAuth2AuthorizedClientService(
    private val repository: PersistentOAuth2AuthorizedClientRepository,
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository
) : ReactiveOAuth2AuthorizedClientService {

    override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
        clientRegistrationId: String,
        principalName: String
    ): Mono<T> = GlobalScope.mono {
        val clientRegistration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId).awaitSingle()

        withDbContext {
            val persistentClient = repository.findByClientRegistrationIdAndUserName(clientRegistrationId, principalName)
            persistentClient?.let {
                OAuth2AuthorizedClient(
                    clientRegistration,
                    principalName,
                    OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        persistentClient.accessToken,
                        persistentClient.accessTokenIssuedAt,
                        persistentClient.accessTokenExpiresAt,
                        persistentClient.accessTokenScopes
                    ),
                    persistentClient.refreshToken?.let {
                        OAuth2RefreshToken(
                            persistentClient.refreshToken,
                            persistentClient.refreshTokenIssuedAt
                        )
                    })
            } as T
        }
    }

    override fun removeAuthorizedClient(clientRegistrationId: String, principalName: String): Mono<Void> = voidMono {
        withDbContext { repository.deleteByClientRegistrationIdAndUserName(clientRegistrationId, principalName) }
    }

    override fun saveAuthorizedClient(
        authorizedClient: OAuth2AuthorizedClient,
        principal: Authentication
    ): Mono<Void> = voidMono {
        withDbContext {
            val userName = principal.name
            val clientRegistrationId = authorizedClient.clientRegistration.registrationId
            val accessToken = authorizedClient.accessToken
            val refreshToken = authorizedClient.refreshToken
            repository.deleteByClientRegistrationIdAndUserName(clientRegistrationId, userName)
            repository.save(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = clientRegistrationId,
                    userName = userName,
                    accessToken = accessToken.tokenValue,
                    accessTokenExpiresAt = accessToken.expiresAt,
                    accessTokenIssuedAt = accessToken.issuedAt,
                    refreshToken = refreshToken?.tokenValue,
                    refreshTokenIssuedAt = refreshToken?.issuedAt,
                    accessTokenScopes = accessToken.scopes
                )
            )
        }
    }
}