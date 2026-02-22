package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.ClientTokenScope
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.coroutineContext
import kotlin.streams.asSequence

private const val STATE_TOKEN_LENGTH = 20L

/**
 * Responsible for OAuth2 client authorization.
 */
@Service
class OAuth2ClientAuthorizationProvider(
    private val savedRequestRepository: SavedAuthorizationRequestRepository,
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val userService: PlatformUsersService,
    private val accessTokenResponseClient: ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>,
    private val persistentAuthorizedClientRepository: PersistentOAuth2AuthorizedClientRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    /**
     * Creates a URL for initialization of OAuth2 authorization code grant flow.
     * User can be redirected to this URL to provide their consent and allow resources access.
     *
     * After authorization, user will be redirected to the callback URL handled by
     * [io.orangebuffalo.simpleaccounting.business.api.CompleteOAuth2FlowMutation].
     */
    suspend fun buildAuthorizationUrl(
        clientRegistrationId: String,
        additionalParameters: Map<String, String> = emptyMap()
    ): String {
        val clientRegistration = getClientRegistration(clientRegistrationId)
        val currentUser = userService.getCurrentUser()
        val state = createStateToken(currentUser)
        val authorizationRequest = buildAuthorizationRequest(clientRegistration, state, additionalParameters)
        savedRequestRepository.save(
            SavedAuthorizationRequest(
                owner = currentUser,
                clientRegistrationId = clientRegistrationId,
                state = state,
                request = authorizationRequest
            )
        )
        return UriComponentsBuilder
            .fromUriString(authorizationRequest.authorizationRequestUri)
            .build(true)
            .toUriString()
    }

    private fun createStateToken(currentUser: PlatformUser): String {
        val randomString = ThreadLocalRandom.current()
            .ints(STATE_TOKEN_LENGTH, 0, charPool.size)
            .asSequence()
            .map(charPool::get)
            .joinToString("")

        val rawToken = "${currentUser.id}:${randomString}"
        return String(Base64.getEncoder().encode(rawToken.toByteArray(StandardCharsets.UTF_8)))
    }

    private suspend fun getClientRegistration(clientRegistrationId: String): ClientRegistration {
        return clientRegistrationRepository.findByRegistrationId(clientRegistrationId)
            .awaitFirstOrNull()
            ?: throw IllegalArgumentException("$clientRegistrationId is not known")
    }

    private fun buildAuthorizationRequest(
        clientRegistration: ClientRegistration,
        state: String,
        additionalParameters: Map<String, String>
    ): OAuth2AuthorizationRequest {

        val builder = when (clientRegistration.authorizationGrantType) {
            AuthorizationGrantType.AUTHORIZATION_CODE -> OAuth2AuthorizationRequest.authorizationCode()
            else -> throw IllegalArgumentException(
                "Invalid Authorization Grant Type (${clientRegistration.authorizationGrantType.value}) " +
                        "for Client Registration with Id: ${clientRegistration.registrationId}"
            )
        }

        return builder
            .clientId(clientRegistration.clientId)
            .authorizationUri(clientRegistration.providerDetails.authorizationUri)
            .additionalParameters(additionalParameters)
            .redirectUri(clientRegistration.redirectUri)
            .scopes(clientRegistration.scopes)
            .state(state)
            .build()
    }

    /**
     * Handles the authorization code grant response from the authorization server.
     *
     * If authorization is successful, [OAuth2SucceededEvent] is emitted. If authorization is not granted,
     * [OAuth2FailedEvent] is emitted.
     */
    suspend fun handleAuthorizationResponse(callbackRequest: OAuth2AuthorizationCallbackRequest) {
        val savedRequest = savedRequestRepository.findByStateAndRemove(callbackRequest.state)

        if (callbackRequest.error != null || callbackRequest.code == null) {
            publishFailedAuthEvent(savedRequest)
            throw OAuth2AuthorizationException(
                OAuth2Error(callbackRequest.error), "Authorization failed with error ${callbackRequest.error}"
            )
        } else {
            handleSuccessfulAuthorization(savedRequest, callbackRequest.code)
        }
    }

    private suspend fun publishFailedAuthEvent(savedRequest: SavedAuthorizationRequest) = eventPublisher.publishEvent(
        OAuth2FailedEvent(
            user = savedRequest.owner,
            clientRegistrationId = savedRequest.clientRegistrationId,
            context = coroutineContext
        )
    )

    private suspend fun handleSuccessfulAuthorization(
        savedRequest: SavedAuthorizationRequest,
        code: String
    ) {
        val clientRegistration = getClientRegistration(savedRequest.clientRegistrationId)
        val authorizationRequest = savedRequest.request

        val codeGrantRequest = OAuth2AuthorizationCodeGrantRequest(
            clientRegistration,
            OAuth2AuthorizationExchange(
                authorizationRequest,
                OAuth2AuthorizationResponse.success(code)
                    .redirectUri(authorizationRequest.redirectUri)
                    .build()
            )
        )

        val tokenResponse = try {
            accessTokenResponseClient.getTokenResponse(codeGrantRequest).awaitSingle()
        } catch (e: OAuth2AuthorizationException) {
            publishFailedAuthEvent(savedRequest)
            throw e
        }

        withDbContext {
            persistentAuthorizedClientRepository.deleteByClientRegistrationIdAndUserName(
                clientRegistration.registrationId, savedRequest.owner.userName
            )

            persistentAuthorizedClientRepository.save(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = clientRegistration.registrationId,
                    accessTokenScopes = authorizationRequest.scopes.map { ClientTokenScope(it) }.toSet(),
                    userName = savedRequest.owner.userName,
                    accessToken = tokenResponse.accessToken.tokenValue,
                    accessTokenIssuedAt = tokenResponse.accessToken.issuedAt,
                    accessTokenExpiresAt = tokenResponse.accessToken.expiresAt,
                    refreshToken = tokenResponse.refreshToken?.tokenValue,
                    refreshTokenIssuedAt = tokenResponse.refreshToken?.issuedAt
                )
            )
        }

        eventPublisher.publishEvent(
            OAuth2SucceededEvent(
                user = savedRequest.owner,
                clientRegistrationId = savedRequest.clientRegistrationId,
                context = coroutineContext
            )
        )
    }
}

data class OAuth2AuthorizationCallbackRequest(
    val code: String?,
    val error: String?,
    val state: String
)
