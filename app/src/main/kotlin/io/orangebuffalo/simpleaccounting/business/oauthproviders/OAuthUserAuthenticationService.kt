package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.security.authentication.UserNotActivatedException
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingProperties
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import mu.KotlinLogging
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.security.MessageDigest
import java.time.Duration
import java.nio.charset.StandardCharsets

/**
 * Path of the frontend page the authorization servers redirect the users back to.
 */
const val OAUTH_IDENTITY_CALLBACK_PATH = "/oauth-identity-callback"

val OAUTH_FLOW_LIFETIME: Duration = Duration.ofMinutes(10)

private val logger = KotlinLogging.logger {}

/**
 * Authenticates users with the OAuth2 providers registered by the admins, and links
 * identities at those providers to the user profiles.
 *
 * This is unrelated to [io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2ClientAuthorizationProvider],
 * which authorizes this application to access third party APIs on behalf of the users.
 *
 * Account activation is enforced here as it is for password login. The temporary locks applied by
 * [io.orangebuffalo.simpleaccounting.business.security.authentication.AuthenticationService] are
 * deliberately not: they throttle password guessing against this application, while here the
 * credentials are verified by the authorization server.
 */
@Service
class OAuthUserAuthenticationService(
    private val providersService: OAuthProvidersService,
    private val identitiesRepository: UserOAuthIdentitiesRepository,
    private val authenticationRequestsRepository: OAuthAuthenticationRequestsRepository,
    private val platformUsersRepository: PlatformUsersRepository,
    private val accessTokenResponseClient: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>,
    private val timeService: TimeService,
    private val tokenGenerator: TokenGenerator,
    private val properties: SimpleAccountingProperties,
) {

    private val userInfoService = DefaultOAuth2UserService()

    /**
     * Resolves the ways the user with the provided name can authenticate with.
     *
     * For unknown users, password login is reported to avoid disclosing which accounts exist.
     * The same applies to accounts that are not activated yet: the activation state is only
     * disclosed once the credentials are submitted.
     */
    fun getAuthenticationMethods(userName: String): List<UserAuthenticationMethod> {
        val user = platformUsersRepository.findByUserName(userName)
            ?: return listOf(UserAuthenticationMethod.PasswordMethod)
        return getAuthenticationMethods(user)
    }

    /**
     * As soon as a user has linked at least one identity, password login is no longer available for them.
     */
    fun getAuthenticationMethods(user: PlatformUser): List<UserAuthenticationMethod> {
        if (!user.activated) {
            return listOf(UserAuthenticationMethod.PasswordMethod)
        }
        val identities = identitiesRepository.findByUserId(user.id!!)
        if (identities.isEmpty()) {
            return listOf(UserAuthenticationMethod.PasswordMethod)
        }
        return identities
            .map { providersService.getProviderById(it.providerId) }
            .sortedBy { it.name }
            .map { UserAuthenticationMethod.OAuthMethod(providerId = it.id!!, providerName = it.name) }
    }

    fun hasLinkedIdentities(user: PlatformUser): Boolean =
        identitiesRepository.findByUserId(user.id!!).isNotEmpty()

    fun isPasswordLoginAllowed(user: PlatformUser): Boolean = !hasLinkedIdentities(user)

    fun getLinkedIdentities(user: PlatformUser): List<UserOAuthIdentity> =
        identitiesRepository.findByUserId(user.id!!)

    /**
     * Initiates the authorization code flow that logs the user in with a previously linked identity.
     * @throws OAuthUserAuthenticationException.LoginNotAvailableException if the user has no identity at this provider
     * @throws UserNotActivatedException if the account has not been activated yet
     */
    fun startLogin(
        userName: String,
        providerId: String,
        issueRefreshTokenCookie: Boolean,
        currentBrowserBinding: String?,
    ): StartedOAuthFlow {
        val user = platformUsersRepository.findByUserName(userName)
            ?: throw OAuthUserAuthenticationException.LoginNotAvailableException(userName, providerId)
        identitiesRepository.findByUserIdAndProviderId(user.id!!, providerId)
            ?: throw OAuthUserAuthenticationException.LoginNotAvailableException(userName, providerId)
        validateActivated(user)

        return startAuthorization(
            provider = providersService.getProviderById(providerId),
            user = user,
            purpose = OAuthAuthenticationPurpose.LOGIN,
            issueRefreshTokenCookie = issueRefreshTokenCookie,
            currentBrowserBinding = currentBrowserBinding,
        )
    }

    /**
     * Initiates the authorization code flow that links a new identity to the current user profile.
     * @throws OAuthUserAuthenticationException.ProviderAlreadyLinkedException if an identity is already linked
     */
    fun startLinking(providerId: String, currentBrowserBinding: String?): StartedOAuthFlow {
        val user = getCurrentUser()
        if (identitiesRepository.findByUserIdAndProviderId(user.id!!, providerId) != null) {
            throw OAuthUserAuthenticationException.ProviderAlreadyLinkedException(providerId)
        }

        return startAuthorization(
            provider = providersService.getProviderById(providerId),
            user = user,
            purpose = OAuthAuthenticationPurpose.LINK,
            issueRefreshTokenCookie = false,
            currentBrowserBinding = currentBrowserBinding,
        )
    }

    /**
     * Removes the identity linked at the provided provider from the current user profile.
     */
    fun unlinkIdentity(providerId: String) {
        val user = getCurrentUser()
        val identity = identitiesRepository.findByUserIdAndProviderId(user.id!!, providerId)
            ?: throw EntityNotFoundException("Identity at provider $providerId is not linked to the current user")
        identitiesRepository.delete(identity)
    }

    /**
     * Completes the flow started by [startLogin] or [startLinking] by exchanging the authorization code
     * for an access token and retrieving the identity of the user at the provider.
     *
     * [browserBinding] is the secret this application has handed to the browser when the flow was
     * started; it is required so that a party that only knows the state - the authorization server
     * itself, first of all - cannot complete the flow.
     */
    fun completeAuthentication(
        state: String,
        browserBinding: String?,
        code: String?,
        error: String?,
    ): CompletedOAuthAuthentication {
        val request = consumeAuthenticationRequest(state, browserBinding)
        if (error != null || code == null) {
            throw OAuthUserAuthenticationException.AuthorizationFailedException(
                error ?: "authorization code is not provided"
            )
        }

        val provider = providersService.getProviderById(request.providerId)
        val externalId = resolveExternalId(provider, code)
        val user = platformUsersRepository.findById(request.userId)
            // should never happen due to DB constraints
            .orElseThrow { IllegalStateException("User ${request.userId} is not found") }

        return when (request.purpose) {
            OAuthAuthenticationPurpose.LOGIN -> completeLogin(request, user, externalId)
            OAuthAuthenticationPurpose.LINK -> completeLinking(provider, user, externalId)
        }
    }

    private fun completeLogin(
        request: OAuthAuthenticationRequest,
        user: PlatformUser,
        externalId: String,
    ): CompletedOAuthAuthentication {
        val identity = identitiesRepository.findByUserIdAndProviderId(user.id!!, request.providerId)
        if (identity == null || identity.externalId != externalId) {
            throw OAuthUserAuthenticationException.IdentityMismatchException()
        }
        validateActivated(user)
        return CompletedOAuthAuthentication(
            purpose = OAuthAuthenticationPurpose.LOGIN,
            user = user,
            issueRefreshTokenCookie = request.issueRefreshTokenCookie,
        )
    }

    private fun completeLinking(
        provider: OAuthProvider,
        user: PlatformUser,
        externalId: String,
    ): CompletedOAuthAuthentication {
        if (identitiesRepository.findByUserIdAndProviderId(user.id!!, provider.id!!) != null) {
            throw OAuthUserAuthenticationException.ProviderAlreadyLinkedException(provider.id)
        }
        if (identitiesRepository.findByProviderIdAndExternalId(provider.id, externalId) != null) {
            throw OAuthUserAuthenticationException.IdentityAlreadyInUseException()
        }
        identitiesRepository.save(
            UserOAuthIdentity(
                userId = user.id!!,
                providerId = provider.id,
                externalId = externalId,
            )
        )
        return CompletedOAuthAuthentication(
            purpose = OAuthAuthenticationPurpose.LINK,
            user = user,
            issueRefreshTokenCookie = false,
        )
    }

    private fun validateActivated(user: PlatformUser) {
        if (!user.activated) {
            throw UserNotActivatedException()
        }
    }

    private fun startAuthorization(
        provider: OAuthProvider,
        user: PlatformUser,
        purpose: OAuthAuthenticationPurpose,
        issueRefreshTokenCookie: Boolean,
        currentBrowserBinding: String?,
    ): StartedOAuthFlow {
        authenticationRequestsRepository.deleteExpired(timeService.currentTime())
        // starting a flow is anonymous, so only the caller's own pending request may be discarded:
        // otherwise anyone knowing a username could abort the login of the legitimate browser
        currentBrowserBinding?.let { authenticationRequestsRepository.deleteByBrowserBinding(it) }

        val state = tokenGenerator.generateSecureToken()
        val browserBinding = tokenGenerator.generateSecureToken()
        val authorizationRequest = buildAuthorizationRequest(provider, state)

        authenticationRequestsRepository.save(
            OAuthAuthenticationRequest(
                state = state,
                browserBinding = browserBinding,
                providerId = provider.id!!,
                userId = user.id!!,
                purpose = purpose,
                issueRefreshTokenCookie = issueRefreshTokenCookie,
                expiresAt = timeService.currentTime().plus(OAUTH_FLOW_LIFETIME),
            )
        )

        return StartedOAuthFlow(
            authorizationUrl = UriComponentsBuilder
                .fromUriString(authorizationRequest.authorizationRequestUri)
                .build(true)
                .toUriString(),
            browserBinding = browserBinding,
        )
    }

    private fun buildAuthorizationRequest(provider: OAuthProvider, state: String?): OAuth2AuthorizationRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .clientId(provider.clientId)
            .authorizationUri(provider.authorizationUrl)
            .redirectUri(redirectUri())
            .scopes(provider.scopes.map { it.scope }.toSet())
            .also { if (state != null) it.state(state) }
            .build()

    private fun consumeAuthenticationRequest(state: String, browserBinding: String?): OAuthAuthenticationRequest {
        val request = authenticationRequestsRepository.findByState(state)
            ?: throw OAuthUserAuthenticationException.UnknownAuthorizationRequestException()

        // verified before the request is consumed, so that a party knowing only the state
        // cannot abort the flow of the legitimate browser
        if (!matchesBinding(request.browserBinding, browserBinding)) {
            logger.warn { "OAuth flow ${request.id} is completed without a matching browser binding" }
            throw OAuthUserAuthenticationException.UnknownAuthorizationRequestException()
        }

        val consumedRows = authenticationRequestsRepository.deleteByState(state)
        if (consumedRows == 0) {
            // a concurrent callback has already consumed this request
            throw OAuthUserAuthenticationException.UnknownAuthorizationRequestException()
        }
        if (!request.expiresAt.isAfter(timeService.currentTime())) {
            throw OAuthUserAuthenticationException.UnknownAuthorizationRequestException()
        }
        return request
    }

    private fun matchesBinding(expected: String, presented: String?): Boolean {
        if (presented == null) return false
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            presented.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun resolveExternalId(provider: OAuthProvider, code: String): String {
        val clientRegistration = provider.toClientRegistration()
        val tokenResponse = try {
            accessTokenResponseClient.getTokenResponse(
                OAuth2AuthorizationCodeGrantRequest(
                    clientRegistration,
                    OAuth2AuthorizationExchange(
                        buildAuthorizationRequest(provider, state = null),
                        OAuth2AuthorizationResponse.success(code)
                            .redirectUri(redirectUri())
                            .build()
                    )
                )
            )
        } catch (e: OAuth2AuthorizationException) {
            logger.warn(e) { "Failed to exchange the authorization code at provider ${provider.id}" }
            throw OAuthUserAuthenticationException.AuthorizationFailedException("token endpoint request failed")
        }

        return try {
            userInfoService.loadUser(OAuth2UserRequest(clientRegistration, tokenResponse.accessToken)).name
        } catch (e: OAuth2AuthenticationException) {
            logger.warn(e) { "Failed to load the user info from provider ${provider.id}" }
            throw OAuthUserAuthenticationException.AuthorizationFailedException("user info request failed")
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "User info from provider ${provider.id} has no ${provider.userIdAttribute} attribute" }
            throw OAuthUserAuthenticationException.AuthorizationFailedException(
                "user info has no '${provider.userIdAttribute}' attribute"
            )
        }
    }

    private fun OAuthProvider.toClientRegistration(): ClientRegistration = ClientRegistration
        .withRegistrationId(id!!)
        .clientName(name)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(redirectUri())
        .scope(scopes.map { it.scope })
        .authorizationUri(authorizationUrl)
        .tokenUri(tokenUrl)
        .userInfoUri(userInfoUrl)
        .userNameAttributeName(userIdAttribute)
        .build()

    private fun redirectUri() = "${properties.publicUrl}$OAUTH_IDENTITY_CALLBACK_PATH"

    private fun getCurrentUser(): PlatformUser {
        val userName = ensureRegularUserPrincipal().userName
        return platformUsersRepository.findByUserName(userName)
            ?: throw IllegalStateException("Current principal is not resolved to a user")
    }
}

/**
 * A way for a particular user to authenticate with the application.
 */
sealed interface UserAuthenticationMethod {
    data object PasswordMethod : UserAuthenticationMethod
    data class OAuthMethod(val providerId: String, val providerName: String) : UserAuthenticationMethod
}

data class StartedOAuthFlow(
    val authorizationUrl: String,
    val browserBinding: String,
)

data class CompletedOAuthAuthentication(
    val purpose: OAuthAuthenticationPurpose,
    val user: PlatformUser,
    val issueRefreshTokenCookie: Boolean,
)
