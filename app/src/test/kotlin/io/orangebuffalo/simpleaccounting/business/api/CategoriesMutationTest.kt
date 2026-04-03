package io.orangebuffalo.simpleaccounting.business.api

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
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

class CategoriesMutationTest(
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
    @DisplayName("createCategory mutation")
    inner class CreateCategoryMutation {

        @Nested
        @DisplayName("Authorization")
        inner class Authorization {

            @Test
            fun `should return NOT_AUTHORIZED error for anonymous requests`() {
                client
                    .graphqlMutation { createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!.toInt()) }
                    .fromAnonymous()
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCategory)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for admin user`() {
                client
                    .graphqlMutation { createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!.toInt()) }
                    .from(preconditions.farnsworth)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCategory)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for workspace access token`() {
                client
                    .graphqlMutation { createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!.toInt()) }
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
                    createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!.toInt(), name = value)
                },
                sizeConstraintTestCases("name", maxLength = 255) { value ->
                    createCategoryMutation(workspaceId = preconditions.fryWorkspace.id!!.toInt(), name = value)
                },
                sizeConstraintTestCases("description", maxLength = 1000) { value ->
                    createCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                        description = value,
                    )
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
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
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
                    .filter { it.workspaceId == preconditions.fryWorkspace.id }
                    .shouldBeSingle()
                    .also {
                        it.name.shouldBe("Robot oil")
                        it.description.shouldBe("Maintenance supplies for Bender")
                        it.income.shouldBe(false)
                        it.expense.shouldBe(true)
                    }
            }

            @Test
            fun `should create a category without optional description`() {
                client
                    .graphqlMutation {
                        createCategoryMutation(
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
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
                        createCategoryMutation(workspaceId = preconditions.zoidbergWorkspace.id!!.toInt())
                    }
                    .from(preconditions.fry)
                    .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateCategory)
            }
        }
    }

    @Nested
    @DisplayName("editCategory mutation")
    inner class EditCategoryMutation {

        @Nested
        @DisplayName("Authorization")
        inner class Authorization {

            @Test
            fun `should return NOT_AUTHORIZED error for anonymous requests`() {
                client
                    .graphqlMutation {
                        editCategoryMutation(
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
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
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
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
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
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
                        workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                        id = category(workspace = preconditions.fryWorkspace).id!!.toInt(),
                        name = value,
                    )
                },
                sizeConstraintTestCases("name", maxLength = 255) { value ->
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                        id = category(workspace = preconditions.fryWorkspace).id!!.toInt(),
                        name = value,
                    )
                },
                sizeConstraintTestCases("description", maxLength = 1000) { value ->
                    editCategoryMutation(
                        workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                        id = category(workspace = preconditions.fryWorkspace).id!!.toInt(),
                        description = value,
                    )
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
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                            id = category.id!!.toInt(),
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

                aggregateTemplate.findAll<Category>()
                    .filter { it.id == category.id }
                    .shouldBeSingle()
                    .also {
                        it.name.shouldBe("Spaceship fuel")
                        it.description.shouldBe("Delivery to Omicron Persei 8")
                        it.income.shouldBe(false)
                        it.expense.shouldBe(true)
                    }
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
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                            id = category.id!!.toInt(),
                            name = "Slurm supplies",
                        )
                    }
                    .from(preconditions.fry)
                    .executeAndVerifySuccessResponse(
                        DgsConstants.MUTATION.EditCategory to buildJsonObject {
                            put("description", JsonNull)
                        }
                    )
            }

            @Test
            fun `should return entity not found error for non-existent category`() {
                client
                    .graphqlMutation {
                        editCategoryMutation(
                            workspaceId = preconditions.fryWorkspace.id!!.toInt(),
                            id = Int.MAX_VALUE,
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
                            workspaceId = preconditions.zoidbergWorkspace.id!!.toInt(),
                            id = zoidbergCategory.id!!.toInt(),
                        )
                    }
                    .from(preconditions.fry)
                    .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditCategory)
            }
        }
    }

    private fun MutationProjection.createCategoryMutation(
        workspaceId: Int,
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
        id
        this.name
        this.description
        this.income
        this.expense
    }

    private fun MutationProjection.editCategoryMutation(
        workspaceId: Int,
        id: Int,
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
