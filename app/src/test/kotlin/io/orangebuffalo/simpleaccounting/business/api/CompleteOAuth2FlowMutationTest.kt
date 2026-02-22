package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2AuthorizationCallbackRequest
import io.orangebuffalo.simpleaccounting.infra.oauth2.OAuth2ClientAuthorizationProvider
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

class CompleteOAuth2FlowMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var authorizationProvider: OAuth2ClientAuthorizationProvider

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
        }
    }

    @Test
    fun `should complete OAuth2 flow successfully for anonymous user`() {
        client
            .graphqlMutation { completeOAuth2FlowMutation(code = "code", error = null, state = "state") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "completeOAuth2Flow" to buildJsonObject {
                    put("success", true)
                    put("errorId", JsonNull)
                }
            )

        verifyBlocking(authorizationProvider) {
            handleAuthorizationResponse(
                OAuth2AuthorizationCallbackRequest(
                    code = "code",
                    error = null,
                    state = "state"
                )
            )
        }
    }

    @Test
    fun `should complete OAuth2 flow successfully for authenticated user`() {
        client
            .graphqlMutation { completeOAuth2FlowMutation(code = "code", error = null, state = "state") }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                "completeOAuth2Flow" to buildJsonObject {
                    put("success", true)
                    put("errorId", JsonNull)
                }
            )
    }

    @Test
    fun `should return failure response with errorId when provider throws exception`() {
        whenever(tokenGenerator.generateUuid()) doReturn "test-error-id"
        authorizationProvider.stub {
            onBlocking { handleAuthorizationResponse(any()) } doThrow IllegalStateException("State is not known")
        }

        client
            .graphqlMutation { completeOAuth2FlowMutation(code = "code", error = null, state = "unknown-state") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "completeOAuth2Flow" to buildJsonObject {
                    put("success", false)
                    put("errorId", "test-error-id")
                }
            )
    }

    @Test
    fun `should return failure response with errorId when provider receives error response`() {
        whenever(tokenGenerator.generateUuid()) doReturn "test-error-id"
        authorizationProvider.stub {
            onBlocking { handleAuthorizationResponse(any()) } doThrow RuntimeException("Authorization failed with error access_denied")
        }

        client
            .graphqlMutation { completeOAuth2FlowMutation(code = null, error = "access_denied", state = "state") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "completeOAuth2Flow" to buildJsonObject {
                    put("success", false)
                    put("errorId", "test-error-id")
                }
            )
    }

    private fun MutationProjection.completeOAuth2FlowMutation(
        code: String?,
        error: String?,
        state: String
    ): MutationProjection = completeOAuth2Flow(code = code, error = error, state = state) {
        success
        errorId
    }
}
