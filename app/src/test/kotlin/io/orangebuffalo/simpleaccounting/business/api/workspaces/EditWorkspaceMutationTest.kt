package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.kotest.matchers.shouldBe
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
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("editWorkspace mutation")
class EditWorkspaceMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry, defaultCurrency = "USD", residency = "US")
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
            countryCodeTestCases { value -> editWorkspaceMutation(id = preconditions.fryWorkspace.id!!, residency = value) }
                .filterNot { it is GraphqlMutationValidBoundaryTestCase },
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
        fun `should update the workspace name and residency`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(
                        id = preconditions.fryWorkspace.id!!,
                        name = "New New York Express",
                        residency = "PA",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditWorkspace to buildJsonObject {
                        put("id", preconditions.fryWorkspace.id!!)
                        put("name", "New New York Express")
                        put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                        put("residency", "PA")
                    }
                )

            aggregateTemplate.findSingle<Workspace>(preconditions.fryWorkspace.id!!)
                .shouldBeEntityWithFields(
                    Workspace(
                        name = "New New York Express",
                        defaultCurrency = preconditions.fryWorkspace.defaultCurrency,
                        ownerId = preconditions.fry.id!!,
                        residency = "PA",
                    )
                )
        }

        @ParameterizedTest
        @ValueSource(strings = ["AU", "DE"])
        fun `should reject incompatible residency using stored currency without updating the workspace`(residency: String) {
            val before = preconditions.fryWorkspace
            client.graphqlMutation {
                editWorkspaceMutation(id = before.id!!, name = "Robot Arms Apts", residency = residency)
            }
                .from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(
                    errorCode = "INCOMPATIBLE_CURRENCY",
                    path = DgsConstants.MUTATION.EditWorkspace,
                )
            aggregateTemplate.findSingle<Workspace>(before.id!!).shouldBe(before)
        }

        @Test
        fun `should allow correcting an existing incompatible residency`() {
            val workspace = preconditions { workspace(owner = preconditions.fry, defaultCurrency = "USD", residency = "AU") }
            client.graphqlMutation {
                editWorkspaceMutation(id = workspace.id!!, version = workspace.version!!, residency = "US")
            }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditWorkspace to buildJsonObject {
                        put("id", workspace.id!!)
                        put("name", "Planet Express")
                        put("defaultCurrency", "USD")
                        put("residency", "US")
                    }
                )
            aggregateTemplate.findSingle<Workspace>(workspace.id!!).residency.shouldBe("US")
        }

        @Test
        fun `should return entity not found error for workspace of another user`() {
            val zoidbergWorkspace = preconditions { workspace(owner = preconditions.zoidberg) }

            client
                .graphqlMutation {
                    editWorkspaceMutation(id = zoidbergWorkspace.id!!, residency = "DE")
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditWorkspace)

            aggregateTemplate.findSingle<Workspace>(zoidbergWorkspace.id!!).shouldBe(zoidbergWorkspace)
        }

        @Test
        fun `should reject stale version before checking currency compatibility`() {
            val workspace = preconditions.fryWorkspace
            client.graphqlMutation {
                editWorkspaceMutation(id = workspace.id!!, version = workspace.version!! - 1, residency = "AU")
            }
                .from(preconditions.fry)
                .executeAndVerifySubmittedOutdatedStateError(path = DgsConstants.MUTATION.EditWorkspace)
            aggregateTemplate.findSingle<Workspace>(workspace.id!!).shouldBe(workspace)
        }

        @Test
        fun `should return entity not found error for non-existent workspace`() {
            client
                .graphqlMutation {
                    editWorkspaceMutation(id = "missing-id")
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditWorkspace)
        }
    }

    private fun MutationProjection.editWorkspaceMutation(
        id: String,
        version: Int = preconditions.fryWorkspace.version!!,
        name: String = "Planet Express",
        residency: String = "US",
    ): MutationProjection = editWorkspace(
        id = id,
        version = version,
        name = name,
        residency = residency,
    ) {
        this.id
        this.name
        this.defaultCurrency
        this.residency
    }
}
