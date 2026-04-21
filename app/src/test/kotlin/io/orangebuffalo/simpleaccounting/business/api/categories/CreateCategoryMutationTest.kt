package io.orangebuffalo.simpleaccounting.business.api.categories

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
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

@DisplayName("createCategory mutation")
class CreateCategoryMutationTest(
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
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCategory)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCategory)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCategory)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value ->
                createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!, name = value)
            },
            sizeConstraintTestCases("name", maxLength = 255) { value ->
                createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!, name = value)
            },
            sizeConstraintTestCases("description", maxLength = 1000) { value ->
                createCategoryMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    description = value,
                )
            },
            requiredFieldRejectedTestCases("expense") {
                createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("income") {
                createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateCategory)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create a new category with all fields`() {
            client
                .graphqlMutation {
                    createCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        name = "Robot oil",
                        description = "Maintenance supplies for Bender",
                        income = false,
                        expense = true,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateCategory to buildJsonObject {
                        put("name", "Robot oil")
                        put("description", "Maintenance supplies for Bender")
                        put("income", false)
                        put("expense", true)
                    }
                )

            aggregateTemplate.findAll<Category>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.name == "Robot oil" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Category(
                        name = "Robot oil",
                        workspaceId = preconditions.fryWorkspace.id!!,
                        description = "Maintenance supplies for Bender",
                        income = false,
                        expense = true,
                    )
                )
        }

        @Test
        fun `should create a category without optional description`() {
            client
                .graphqlMutation {
                    createCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        name = "Slurm supplies",
                        income = true,
                        expense = false,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateCategory to buildJsonObject {
                        put("name", "Slurm supplies")
                        put("description", JsonNull)
                        put("income", true)
                        put("expense", false)
                    }
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createCategoryMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateCategory)
        }
    }

    private fun MutationProjection.createCategoryMutation(
        workspaceId: Long,
        name: String = "Delivery",
        description: String? = null,
        income: Boolean = true,
        expense: Boolean = true,
    ): MutationProjection = createCategory(
        workspaceId = workspaceId,
        name = name,
        description = description,
        income = income,
        expense = expense,
    ) {
        this.name
        this.description
        this.income
        this.expense
    }
}
