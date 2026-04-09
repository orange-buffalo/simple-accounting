package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("saveSharedWorkspace mutation")
class SaveSharedWorkspaceMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val farnsworth = farnsworth()
            val zoidberg = zoidberg()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val expiredWorkspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.minusMillis(1),
                token = "expiredToken",
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { saveSharedWorkspaceMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.SaveSharedWorkspace)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { saveSharedWorkspaceMutation() }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.SaveSharedWorkspace)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { saveSharedWorkspaceMutation() }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.SaveSharedWorkspace)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = mustNotBeBlankTestCases("token", boundarySetup = ::setupBoundaryData) { value ->
            saveSharedWorkspaceMutation(token = value)
        }

        private fun setupBoundaryData() {
            preconditions {
                workspaceAccessToken(
                    workspace = preconditions.fryWorkspace,
                    token = "a",
                    validTill = MOCK_TIME.plusSeconds(10000),
                )
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.SaveSharedWorkspace)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should save shared workspace and return its details`() {
            client
                .graphqlMutation {
                    saveSharedWorkspaceMutation(token = preconditions.workspaceAccessToken.token)
                }
                .from(preconditions.zoidberg)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.SaveSharedWorkspace to buildJsonObject {
                        put("id", preconditions.fryWorkspace.id!!.toInt())
                        put("name", preconditions.fryWorkspace.name)
                        put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                    }
                )

            aggregateTemplate.findAll<SavedWorkspaceAccessToken>()
                .filter { it.ownerId == preconditions.zoidberg.id }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    SavedWorkspaceAccessToken(
                        ownerId = preconditions.zoidberg.id!!,
                        workspaceAccessTokenId = preconditions.workspaceAccessToken.id!!,
                    )
                )
        }

        @Test
        fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error for expired token`() {
            client
                .graphqlMutation {
                    saveSharedWorkspaceMutation(token = preconditions.expiredWorkspaceAccessToken.token)
                }
                .from(preconditions.zoidberg)
                .executeAndVerifyBusinessError(
                    message = "Token ${preconditions.expiredWorkspaceAccessToken.token} is not valid",
                    errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                    path = DgsConstants.MUTATION.SaveSharedWorkspace,
                )
        }

        @Test
        fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error for unknown token`() {
            client
                .graphqlMutation { saveSharedWorkspaceMutation(token = "unknownToken") }
                .from(preconditions.zoidberg)
                .executeAndVerifyBusinessError(
                    message = "Token unknownToken is not valid",
                    errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                    path = DgsConstants.MUTATION.SaveSharedWorkspace,
                )
        }
    }

    private fun MutationProjection.saveSharedWorkspaceMutation(
        token: String = "validToken",
    ): MutationProjection = saveSharedWorkspace(token = token) {
        this.id
        this.name
        this.defaultCurrency
    }
}
