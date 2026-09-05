package io.orangebuffalo.simpleaccounting.business.api.auth

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthAuthenticationPurpose
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
import io.orangebuffalo.simpleaccounting.business.security.authentication.UserNotActivatedException
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CompleteOAuthAuthenticationMutation(
    private val oauthUserAuthenticationService: OAuthUserAuthenticationService,
    private val jwtService: JwtService,
    private val refreshTokensService: RefreshTokensService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription(
        "Completes the OAuth2 flow started by startOAuthLogin or startOAuthIdentityLinking " +
                "by processing the authorization server callback. Requires the browser to present the " +
                "binding cookie issued when the flow was started. " +
                "An access token is only returned for the LOGIN outcome: linking an identity to an " +
                "existing profile never grants a new session."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.UnknownAuthorizationRequestException::class,
        errorCode = "UNKNOWN_AUTHORIZATION_REQUEST",
        errorCodeDescription = "The state does not match any pending authorization request, the request has " +
                "expired or has already been used, or the browser did not present the binding cookie.",
    )
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.AuthorizationFailedException::class,
        errorCode = "AUTHORIZATION_FAILED",
        errorCodeDescription = "The authorization server did not grant the access, the token exchange failed, " +
                "or the identity could not be resolved from the user info response.",
    )
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.IdentityMismatchException::class,
        errorCode = "IDENTITY_MISMATCH",
        errorCodeDescription = "The authenticated identity is not the one linked to the user being logged in.",
    )
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.IdentityAlreadyInUseException::class,
        errorCode = "IDENTITY_ALREADY_IN_USE",
        errorCodeDescription = "The authenticated identity is already linked to another user.",
    )
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.ProviderAlreadyLinkedException::class,
        errorCode = "PROVIDER_ALREADY_LINKED",
        errorCodeDescription = "An identity at this provider is already linked to the user.",
    )
    @BusinessError(
        exceptionClass = UserNotActivatedException::class,
        errorCode = "USER_NOT_ACTIVATED",
        errorCodeDescription = "The user account has not been activated yet.",
    )
    fun completeOAuthAuthentication(
        @GraphQLDescription("The authorization code returned by the authorization server.")
        code: String? = null,
        @GraphQLDescription("The error code returned by the authorization server if authorization failed.")
        error: String? = null,
        @GraphQLDescription("The state token that was included in the authorization request.")
        @NotBlank
        state: String,
        env: DataFetchingEnvironment,
    ): CompleteOAuthAuthenticationResponse {
        val result = oauthUserAuthenticationService.completeAuthentication(
            state = state,
            browserBinding = env.oauthFlowBinding(),
            code = code,
            error = error,
        )
        env.removeOAuthFlowBindingCookie()

        if (result.purpose == OAuthAuthenticationPurpose.LINK) {
            return CompleteOAuthAuthenticationResponse(
                outcome = OAuthAuthenticationOutcome.LINK,
                accessToken = null,
            )
        }

        val principal = result.user.toSecurityPrincipal()
        if (result.issueRefreshTokenCookie) {
            env.addRefreshTokenCookie(refreshTokensService.generateRefreshToken(principal.userName))
        }
        return CompleteOAuthAuthenticationResponse(
            outcome = OAuthAuthenticationOutcome.LOGIN,
            accessToken = jwtService.buildJwtToken(principal),
        )
    }

    @GraphQLDescription("Response for the completeOAuthAuthentication mutation.")
    data class CompleteOAuthAuthenticationResponse(
        @GraphQLDescription("What the completed flow has achieved.")
        val outcome: OAuthAuthenticationOutcome,
        @GraphQLDescription(
            "The JWT access token for the authenticated user. " +
                    "Only provided for the LOGIN outcome."
        )
        val accessToken: String?,
    )

    @GraphQLDescription("The outcome of a completed OAuth2 flow.")
    enum class OAuthAuthenticationOutcome {
        @GraphQLDescription("The user has been logged in.")
        LOGIN,

        @GraphQLDescription("A new identity has been linked to the user profile.")
        LINK,
    }
}
