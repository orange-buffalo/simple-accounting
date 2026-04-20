package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
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

@DisplayName("editWorkspace mutation")
class EditWorkspaceMutationTest(
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
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditWorkspace)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditWorkspace)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditWorkspace)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value ->
                editWorkspaceMutation(id = preconditions.fryWorkspace.id!!, name = value)
            },
            sizeConstraintTestCases("name", maxLength = 255) { value ->
                editWorkspaceMutation(id = preconditions.fryWorkspace.id!!, name = value)
            },
            requiredFieldRejectedTestCases("id") {
                editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditWorkspace)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update the workspace name`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(
                        id = preconditions.fryWorkspace.id!!,
                        name = "New New York Express",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditWorkspace to buildJsonObject {
                        put("id", preconditions.fryWorkspace.id!!.toInt())
                        put("name", "New New York Express")
                        put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                    }
                )

            aggregateTemplate.findSingle<Workspace>(preconditions.fryWorkspace.id!!)
                .shouldBeEntityWithFields(
                    Workspace(
                        name = "New New York Express",
                        defaultCurrency = preconditions.fryWorkspace.defaultCurrency,
                        ownerId = preconditions.fry.id!!,
                    )
                )
        }

        @Test
        fun `should return entity not found error for workspace of another user`() {
            val zoidbergWorkspace = preconditions { workspace(owner = preconditions.zoidberg) }

            client
                .graphqlMutation {
                    editWorkspaceMutation(id = zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditWorkspace)
        }

        @Test
        fun `should return entity not found error for non-existent workspace`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(id = Long.MAX_VALUE)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditWorkspace)
        }
    }

    private fun MutationProjection.editWorkspaceMutation(
        id: Long,
        name: String = "Planet Express",
    ): MutationProjection = editWorkspace(
        id = id,
        name = name,
    ) {
        this.id
        this.name
        this.defaultCurrency
    }
}
