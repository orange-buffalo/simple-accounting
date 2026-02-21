package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2AuthorizationFlowException
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2AuthorizationCallbackRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2ClientAuthorizationProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CompleteOAuth2FlowMutation(
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Completes the OAuth2 authorization flow by processing the authorization server callback.")
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    @BusinessError(
        exceptionClass = OAuth2AuthorizationFlowException::class,
        errorCode = "AUTHORIZATION_FAILED",
        description = "The OAuth2 authorization flow failed. This can happen due to an invalid state, " +
                "an error response from the authorization server, or a failure to exchange the code for a token.",
    )
    suspend fun completeOAuth2Flow(
        @GraphQLDescription("The authorization code returned by the authorization server.")
        code: String?,
        @GraphQLDescription("The error code returned by the authorization server if authorization failed.")
        error: String?,
        @GraphQLDescription("The state token that was included in the authorization request.")
        state: String,
    ): CompleteOAuth2FlowResponse {
        logger.debug { "Received new OAuth2 authorization callback" }

        try {
            clientAuthorizationProvider.handleAuthorizationResponse(
                OAuth2AuthorizationCallbackRequest(
                    code = code,
                    error = error,
                    state = state,
                )
            )
        } catch (e: Exception) {
            throw OAuth2AuthorizationFlowException("OAuth2 authorization flow failed", e)
        }

        logger.debug { "OAuth2 authorization callback successfully processed" }
        return CompleteOAuth2FlowResponse()
    }

    @GraphQLDescription(
        "Response for the completeOAuth2Flow mutation. " +
                "Always succeeds if no error is returned by standard GraphQL error response structure."
    )
    data class CompleteOAuth2FlowResponse(
        val success: Boolean = true,
    )
}
