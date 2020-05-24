package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.ClientTokenScope
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.services.security.ensureRegularUserPrincipal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.security.SecureRandom
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private const val TOKEN_LENGTH = 20

/**
 * Entry point for OAuth2 integration. Provides
 */
@Service
class OAuth2Service(
    private val requestRepository: Oauth2AuthorizationRequestRepository,
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val userService: PlatformUserService,
    private val accessTokenResponseClient: ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>,
    private val persistentAuthorizedClientRepository: PersistentOAuth2AuthorizedClientRepository,
    private val timeService: TimeService,
    private val authorizedClientRepository: ServerOAuth2AuthorizedClientRepository,
    private val clientService: ReactiveOAuth2AuthorizedClientService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val random = SecureRandom()

    suspend fun buildAuthorizationUrl(clientRegistrationId: String): String {
        val clientRegistration = getClientRegistration(clientRegistrationId)

        val authStateTokenBytes = ByteArray(TOKEN_LENGTH)
        random.nextBytes(authStateTokenBytes)

        val currentUser = userService.getCurrentUser()
        val state = "${currentUser.id}:${String(Base64.getEncoder().encode(authStateTokenBytes))}"

        requestRepository.save(
            OAuth2AuthorizationRequest(
                currentUser.id!!,
                state,
                clientRegistrationId
            )
        )

        val authorizationRequest = buildAuthorizationRequest(clientRegistration, state)

        return UriComponentsBuilder
            .fromUriString(authorizationRequest.authorizationRequestUri)
            .build(true)
            .toUriString()
    }

    private suspend fun getClientRegistration(clientRegistrationId: String): ClientRegistration {
        return clientRegistrationRepository.findByRegistrationId(clientRegistrationId)
            .awaitFirstOrNull()
            ?: throw IllegalArgumentException("$clientRegistrationId is not known")
    }

    private fun buildAuthorizationRequest(
        clientRegistration: ClientRegistration,
        state: String? = null
    ): OAuth2AuthorizationRequest {

        val builder = when {
            AuthorizationGrantType.AUTHORIZATION_CODE == clientRegistration.authorizationGrantType ->
                OAuth2AuthorizationRequest.authorizationCode()
            AuthorizationGrantType.IMPLICIT == clientRegistration.authorizationGrantType ->
                OAuth2AuthorizationRequest.implicit()
            else -> throw IllegalArgumentException(
                "Invalid Authorization Grant Type " +
                        "(${clientRegistration.authorizationGrantType.value}) for Client Registration with Id: " +
                        clientRegistration.registrationId
            )
        }

        return builder
            .clientId(clientRegistration.clientId)
            .authorizationUri(clientRegistration.providerDetails.authorizationUri)
            // todo #85: this is google specific. should be a better way
            .additionalParameters(mapOf("access_type" to "offline"))
            .redirectUri(clientRegistration.redirectUriTemplate)
            .scopes(clientRegistration.scopes)
            .state(state)
            .build()
    }

    suspend fun onAuthCallback(code: String?, error: String?, state: String?) {
        if (state == null) {
            //todo #86: meaningful error handling
            throw IllegalStateException("Something bad happened, sorry :( Please try again")
        }

        val persistentAuthorizationRequest = requestRepository.findByStateAndRemove(state)

        if (error != null || code == null) {
            eventPublisher.publishEvent(
                AuthFailedEvent(
                    userId = persistentAuthorizationRequest.ownerId,
                    clientRegistrationId = persistentAuthorizationRequest.clientRegistrationId,
                    errorCode = error,
                    context = coroutineContext
                )
            )
        } else {
            onSuccessfulAuth(persistentAuthorizationRequest, code)
        }
    }

    private suspend fun onSuccessfulAuth(
        persistentAuthorizationRequest: io.orangebuffalo.simpleaccounting.services.integration.oauth2.OAuth2AuthorizationRequest,
        code: String?
    ) {
        val clientRegistration = getClientRegistration(persistentAuthorizationRequest.clientRegistrationId)
        val authorizationRequest = buildAuthorizationRequest(clientRegistration)

        val codeGrantRequest = OAuth2AuthorizationCodeGrantRequest(
            clientRegistration,
            OAuth2AuthorizationExchange(
                authorizationRequest,
                OAuth2AuthorizationResponse.success(code)
                    .redirectUri(authorizationRequest.redirectUri)
                    .build()
            )
        )

        val tokenResponse = accessTokenResponseClient.getTokenResponse(codeGrantRequest).awaitFirstOrNull()
            ?: throw IllegalStateException("Cannot get token")

        val authorizedUser = userService.getUserByUserId(persistentAuthorizationRequest.ownerId)

        withDbContext {
            persistentAuthorizedClientRepository.deleteByClientRegistrationIdAndUserName(
                clientRegistration.registrationId, authorizedUser.userName
            )

            persistentAuthorizedClientRepository.save(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = clientRegistration.registrationId,
                    accessTokenScopes = authorizationRequest.scopes.map { ClientTokenScope(it) }.toSet(),
                    userName = authorizedUser.userName,
                    accessToken = tokenResponse.accessToken.tokenValue,
                    accessTokenIssuedAt = tokenResponse.accessToken.issuedAt,
                    accessTokenExpiresAt = tokenResponse.accessToken.expiresAt,
                    refreshToken = tokenResponse.refreshToken?.tokenValue,
                    refreshTokenIssuedAt = tokenResponse.refreshToken?.issuedAt
                )
            )
        }

        eventPublisher.publishEvent(
            AuthSucceededEvent(
                user = authorizedUser,
                clientRegistrationId = persistentAuthorizationRequest.clientRegistrationId,
                context = coroutineContext
            )
        )
    }

    // todo #225: evaluate usage of user id to avoid extra db roundtrips
    suspend fun getOAuth2AuthorizedClient(clientRegistrationId: String, userName: String): OAuth2AuthorizedClient? {
        return clientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(clientRegistrationId, userName)
            .awaitFirstOrNull()
            ?.let { client -> if (expiresSoon(client)) null else client }
    }

    private fun expiresSoon(client: OAuth2AuthorizedClient): Boolean {
        val expiresAt = client.accessToken.expiresAt
        return expiresAt != null
                && client.refreshToken == null
                && Duration.between(timeService.currentTime(), expiresAt).get(ChronoUnit.SECONDS) < 60
    }

    fun createWebClient(baseUrl: String): WebClient = WebClient.builder()
        .filter(
            // todo #225: with new version of security library, this can delete the client. we should remove related custom code
            // io/orangebuffalo/simpleaccounting/services/storage/gdrive/GoogleDriveDocumentsStorageService.kt:305
            ServerOAuth2AuthorizedClientExchangeFilterFunction(
                clientRegistrationRepository,
                authorizedClientRepository
            )
        )
        .baseUrl(baseUrl)
        .build()

    suspend fun deleteAuthorizedClient(clientRegistrationId: String) = withDbContext {
        persistentAuthorizedClientRepository.deleteByClientRegistrationIdAndUserName(
            clientRegistrationId, ensureRegularUserPrincipal().userName
        )
    }
}

data class AuthFailedEvent(
    // todo #225: address inconsistency with success event
    val userId: Long,
    val clientRegistrationId: String,
    val errorCode: String?,
    // required to preserve the caller's context, like WebExchange
    val context: CoroutineContext
) {

    fun launchIfClientMatches(clientRegistrationId: String, block: suspend () -> Unit) {
        if (this.clientRegistrationId == clientRegistrationId) {
            GlobalScope.launch(context) { block() }
        }
    }
}

data class AuthSucceededEvent(
    val user: PlatformUser,
    val clientRegistrationId: String,
    // required to preserve the caller's context, like WebExchange
    val context: CoroutineContext
) {

    fun launchIfClientMatches(clientRegistrationId: String, block: suspend () -> Unit) {
        if (this.clientRegistrationId == clientRegistrationId) {
            GlobalScope.launch(context) { block() }
        }
    }
}
