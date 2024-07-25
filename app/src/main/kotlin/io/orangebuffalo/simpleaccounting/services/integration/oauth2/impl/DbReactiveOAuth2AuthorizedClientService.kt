package io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl

import io.orangebuffalo.simpleaccounting.services.integration.oauth2.PersistentOAuth2AuthorizedClientRepository
import io.orangebuffalo.simpleaccounting.infra.voidMono
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import reactor.core.publisher.Mono

/**
 * Persists authorized clients in the database.
 */
class DbReactiveOAuth2AuthorizedClientService(
    private val authorizedClientRepository: PersistentOAuth2AuthorizedClientRepository,
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository
) : ReactiveOAuth2AuthorizedClientService {

    override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
        clientRegistrationId: String,
        principalName: String
    ): Mono<T> = mono {
        val clientRegistration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId).awaitSingle()

        withDbContext {
            val persistentClient = authorizedClientRepository
                .findByClientRegistrationIdAndUserName(clientRegistrationId, principalName)
            @Suppress("UNCHECKED_CAST")
            persistentClient?.let {
                OAuth2AuthorizedClient(
                    clientRegistration,
                    principalName,
                    OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        persistentClient.accessToken,
                        persistentClient.accessTokenIssuedAt,
                        persistentClient.accessTokenExpiresAt,
                        persistentClient.accessTokenScopes.map { it.scope }.toSet()
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
        withDbContext {
            authorizedClientRepository.deleteByClientRegistrationIdAndUserName(clientRegistrationId, principalName)
        }
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
            val existingClient = authorizedClientRepository
                .findByClientRegistrationIdAndUserName(clientRegistrationId, userName)
            if (existingClient == null) {
                authorizedClientRepository.save(
                    PersistentOAuth2AuthorizedClient(
                        clientRegistrationId = clientRegistrationId,
                        userName = userName,
                        accessToken = accessToken.tokenValue,
                        accessTokenExpiresAt = accessToken.expiresAt,
                        accessTokenIssuedAt = accessToken.issuedAt,
                        refreshToken = refreshToken?.tokenValue,
                        refreshTokenIssuedAt = refreshToken?.issuedAt,
                        accessTokenScopes = accessToken.scopes.map { ClientTokenScope(it) }.toSet()
                    )
                )
            } else {
                existingClient.accessToken = accessToken.tokenValue
                existingClient.accessTokenExpiresAt = accessToken.expiresAt
                existingClient.accessTokenIssuedAt = accessToken.issuedAt
                existingClient.accessTokenScopes = accessToken.scopes.map { ClientTokenScope(it) }.toSet()
                if (refreshToken != null) {
                    existingClient.refreshToken = refreshToken.tokenValue
                    existingClient.refreshTokenIssuedAt = refreshToken.issuedAt
                }
                authorizedClientRepository.save(existingClient)
            }
        }
    }
}
