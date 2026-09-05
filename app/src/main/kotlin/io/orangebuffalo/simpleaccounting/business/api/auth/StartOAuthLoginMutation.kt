package io.orangebuffalo.simpleaccounting.business.api.auth

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
import io.orangebuffalo.simpleaccounting.business.security.authentication.UserNotActivatedException
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class StartOAuthLoginMutation(
    private val oauthUserAuthenticationService: OAuthUserAuthenticationService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription(
        "Starts the OAuth2 login flow for the user with the provided name. " +
                "The browser must be redirected to the returned URL; the provider will then redirect " +
                "the user back to the application, where the flow is finished by completeOAuthAuthentication. " +
                "A cookie binding the flow to this browser is issued, and is required to finish the flow."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.LoginNotAvailableException::class,
        errorCode = "LOGIN_NOT_AVAILABLE",
        errorCodeDescription = "The user has no identity linked at the requested provider.",
    )
    @BusinessError(
        exceptionClass = UserNotActivatedException::class,
        errorCode = "USER_NOT_ACTIVATED",
        errorCodeDescription = "The user account has not been activated yet.",
    )
    fun startOAuthLogin(
        @GraphQLDescription("The username of the user attempting to login.")
        @NotBlank
        userName: String,
        @GraphQLDescription("ID of the OAuth2 provider to authenticate with.")
        @NotBlank
        providerId: String,
        @GraphQLDescription(
            "Whether to issue a refresh token cookie for persistent sessions once the login completes. " +
                    "Defaults to false if not provided."
        )
        issueRefreshTokenCookie: Boolean? = null,
        env: DataFetchingEnvironment,
    ): StartOAuthLoginResponse {
        val startedFlow = oauthUserAuthenticationService.startLogin(
            userName = userName,
            providerId = providerId,
            issueRefreshTokenCookie = issueRefreshTokenCookie == true,
            currentBrowserBinding = env.oauthFlowBinding(),
        )
        env.addOAuthFlowBindingCookie(startedFlow.browserBinding)
        return StartOAuthLoginResponse(authorizationUrl = startedFlow.authorizationUrl)
    }

    @GraphQLDescription("Response for the startOAuthLogin mutation.")
    data class StartOAuthLoginResponse(
        @GraphQLDescription("The URL of the authorization server the browser must be redirected to.")
        val authorizationUrl: String,
    )
}
