package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2AuthorizationCallbackRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2ClientAuthorizationProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CompleteOAuth2FlowMutation(
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
    private val tokenGenerator: TokenGenerator,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Completes the OAuth2 authorization flow by processing the authorization server callback.")
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    suspend fun completeOAuth2Flow(
        @GraphQLDescription("The authorization code returned by the authorization server.")
        code: String?,
        @GraphQLDescription("The error code returned by the authorization server if authorization failed.")
        error: String?,
        @GraphQLDescription("The state token that was included in the authorization request.")
        state: String,
    ): CompleteOAuth2FlowResponse {
        logger.debug { "Received new OAuth2 authorization callback" }

        return try {
            clientAuthorizationProvider.handleAuthorizationResponse(
                OAuth2AuthorizationCallbackRequest(
                    code = code,
                    error = error,
                    state = state,
                )
            )
            logger.debug { "OAuth2 authorization callback successfully processed" }
            CompleteOAuth2FlowResponse(success = true)
        } catch (e: Throwable) {
            val errorId = tokenGenerator.generateUuid()
            logger.error(e) { "Failure to process OAuth2 authorization callback. Error ID is $errorId" }
            CompleteOAuth2FlowResponse(success = false, errorId = errorId)
        }
    }

    @GraphQLDescription("Response for the completeOAuth2Flow mutation.")
    data class CompleteOAuth2FlowResponse(
        @GraphQLDescription("Whether the OAuth2 authorization flow was completed successfully.")
        val success: Boolean,
        @GraphQLDescription(
            "An error reference ID that can be used to identify the specific failure in the logs. " +
                    "Present only when the flow failed."
        )
        val errorId: String? = null,
    )
}
