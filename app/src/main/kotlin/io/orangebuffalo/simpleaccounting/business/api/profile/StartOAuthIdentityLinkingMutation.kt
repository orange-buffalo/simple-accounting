package io.orangebuffalo.simpleaccounting.business.api.profile

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.auth.addOAuthFlowBindingCookie
import io.orangebuffalo.simpleaccounting.business.api.auth.oauthFlowBinding
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class StartOAuthIdentityLinkingMutation(
    private val oauthUserAuthenticationService: OAuthUserAuthenticationService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription(
        "Starts the OAuth2 flow that links an identity at the provided provider to the current user profile. " +
                "The browser must be redirected to the returned URL; the provider will then redirect " +
                "the user back to the application, where the flow is finished by completeOAuthAuthentication. " +
                "A cookie binding the flow to this browser is issued, and is required to finish the flow."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    @BusinessError(
        exceptionClass = OAuthUserAuthenticationException.ProviderAlreadyLinkedException::class,
        errorCode = "PROVIDER_ALREADY_LINKED",
        errorCodeDescription = "An identity at this provider is already linked to the current user.",
    )
    fun startOAuthIdentityLinking(
        @GraphQLDescription("ID of the OAuth2 provider to link an identity at.")
        @NotBlank
        providerId: String,
        env: DataFetchingEnvironment,
    ): StartOAuthIdentityLinkingResponse {
        val startedFlow = oauthUserAuthenticationService.startLinking(
            providerId = providerId,
            currentBrowserBinding = env.oauthFlowBinding(),
        )
        env.addOAuthFlowBindingCookie(startedFlow.browserBinding)
        return StartOAuthIdentityLinkingResponse(authorizationUrl = startedFlow.authorizationUrl)
    }

    @GraphQLDescription("Response for the startOAuthIdentityLinking mutation.")
    data class StartOAuthIdentityLinkingResponse(
        @GraphQLDescription("The URL of the authorization server the browser must be redirected to.")
        val authorizationUrl: String,
    )
}
