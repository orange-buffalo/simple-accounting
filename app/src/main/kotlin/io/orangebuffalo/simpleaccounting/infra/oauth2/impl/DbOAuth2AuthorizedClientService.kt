package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.infra.oauth2.PersistentOAuth2AuthorizedClientRepository
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken

class DbOAuth2AuthorizedClientService(
    private val authorizedClientRepository: PersistentOAuth2AuthorizedClientRepository,
    private val clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizedClientService {

    override fun <T : OAuth2AuthorizedClient> loadAuthorizedClient(
        clientRegistrationId: String,
        principalName: String,
    ): T? {
        val clientRegistration = requireNotNull(
            clientRegistrationRepository.findByRegistrationId(clientRegistrationId)
        ) { "$clientRegistrationId is not known" }
        val persistentClient = authorizedClientRepository
            .findByClientRegistrationIdAndUserName(clientRegistrationId, principalName)
            ?: return null
        val authorizedClient = OAuth2AuthorizedClient(
            clientRegistration,
            principalName,
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                persistentClient.accessToken,
                persistentClient.accessTokenIssuedAt,
                persistentClient.accessTokenExpiresAt,
                persistentClient.accessTokenScopes.map { it.scope }.toSet(),
            ),
            persistentClient.refreshToken?.let {
                OAuth2RefreshToken(it, persistentClient.refreshTokenIssuedAt)
            },
        )
        @Suppress("UNCHECKED_CAST")
        return authorizedClient as T
    }

    override fun removeAuthorizedClient(clientRegistrationId: String, principalName: String) {
        authorizedClientRepository.deleteByClientRegistrationIdAndUserName(clientRegistrationId, principalName)
    }

    override fun saveAuthorizedClient(authorizedClient: OAuth2AuthorizedClient, principal: Authentication) {
        val clientRegistrationId = authorizedClient.clientRegistration.registrationId
        val existingClient = authorizedClientRepository
            .findByClientRegistrationIdAndUserName(clientRegistrationId, principal.name)
        val accessToken = authorizedClient.accessToken
        val refreshToken = authorizedClient.refreshToken
        authorizedClientRepository.save(
            existingClient?.copy(
                accessToken = accessToken.tokenValue,
                accessTokenExpiresAt = accessToken.expiresAt,
                accessTokenIssuedAt = accessToken.issuedAt,
                accessTokenScopes = accessToken.scopes.map { ClientTokenScope(it) }.toSet(),
                refreshToken = refreshToken?.tokenValue ?: existingClient.refreshToken,
                refreshTokenIssuedAt = refreshToken?.issuedAt ?: existingClient.refreshTokenIssuedAt,
            ) ?: PersistentOAuth2AuthorizedClient(
                clientRegistrationId = clientRegistrationId,
                userName = principal.name,
                accessToken = accessToken.tokenValue,
                accessTokenExpiresAt = accessToken.expiresAt,
                accessTokenIssuedAt = accessToken.issuedAt,
                refreshToken = refreshToken?.tokenValue,
                refreshTokenIssuedAt = refreshToken?.issuedAt,
                accessTokenScopes = accessToken.scopes.map { ClientTokenScope(it) }.toSet(),
            )
        )
    }
}
