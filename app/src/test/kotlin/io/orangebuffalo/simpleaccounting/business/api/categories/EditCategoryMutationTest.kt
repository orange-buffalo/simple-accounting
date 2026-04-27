package io.orangebuffalo.simpleaccounting.business.api.categories

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("editCategory mutation")
class EditCategoryMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidberg = zoidberg()
            val zoidbergWorkspace = workspace(owner = zoidberg)
            val fryCategory = category(workspace = fryWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditCategory)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditCategory)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditCategory)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value ->
                editCategoryMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryCategory.id!!,
                    name = value,
                )
            },
            sizeConstraintTestCases("name", maxLength = 255) { value ->
                editCategoryMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryCategory.id!!,
                    name = value,
                )
            },
            sizeConstraintTestCases("description", maxLength = 1000) { value ->
                editCategoryMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryCategory.id!!,
                    description = value,
                )
            },
            optionalFieldAbsentTestCases("description") {
                editCategoryMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryCategory.id!!,
                    description = "x",
                )
            },
            requiredFieldRejectedTestCases("id") {
                editCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryCategory.id!!)
            },
            requiredFieldRejectedTestCases("expense") {
                editCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryCategory.id!!)
            },
            requiredFieldRejectedTestCases("income") {
                editCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryCategory.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                editCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryCategory.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditCategory)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update all fields of an existing category`() {
            val category = preconditions { category(workspace = preconditions.fryWorkspace) }

            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = category.id!!,
                        name = "Spaceship fuel",
                        description = "Delivery to Omicron Persei 8",
                        income = false,
                        expense = true,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditCategory to buildJsonObject {
                        put("id", category.id!!.toInt())
                        put("name", "Spaceship fuel")
                        put("description", "Delivery to Omicron Persei 8")
                        put("income", false)
                        put("expense", true)
                    }
                )

            aggregateTemplate.findSingle<Category>(category.id!!)
                .shouldBeEntityWithFields(
                    Category(
                        name = "Spaceship fuel",
                        workspaceId = preconditions.fryWorkspace.id!!,
                        description = "Delivery to Omicron Persei 8",
                        income = false,
                        expense = true,
                    )
                )
        }

        @Test
        fun `should clear optional description`() {
            val category = preconditions {
                category(
                    workspace = preconditions.fryWorkspace,
                    description = "Old description",
                )
            }

            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = category.id!!,
                        name = "Slurm supplies",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditCategory to buildJsonObject {
                        put("id", category.id!!.toInt())
                        put("name", "Slurm supplies")
                        put("description", JsonNull)
                        put("income", true)
                        put("expense", true)
                    }
                )
        }

        @Test
        fun `should return entity not found error for non-existent category`() {
            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditCategory)
        }

        @Test
        fun `should return entity not found error for category in another user workspace`() {
            val zoidbergCategory = preconditions { category(workspace = preconditions.zoidbergWorkspace) }

            client
                .graphqlMutation {
                    editCategoryMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergCategory.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditCategory)
        }
    }

    private fun MutationProjection.editCategoryMutation(
        workspaceId: Long,
        id: Long,
        name: String = "Delivery",
        description: String? = null,
        income: Boolean = true,
        expense: Boolean = true,
    ): MutationProjection = editCategory(
        workspaceId = workspaceId,
        id = id,
        name = name,
        description = description,
        income = income,
        expense = expense,
    ) {
        this.id
        this.name
        this.description
        this.income
        this.expense
    }
}
