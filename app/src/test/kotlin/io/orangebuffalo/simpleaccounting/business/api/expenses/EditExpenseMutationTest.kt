package io.orangebuffalo.simpleaccounting.business.api.expenses

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
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
import java.time.LocalDate

@DisplayName("editExpense mutation")
class EditExpenseMutationTest(
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
            val fryExpense = expense(workspace = fryWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditExpense)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditExpense)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditExpense)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                editExpenseMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryExpense.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                editExpenseMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryExpense.id!!,
                    title = value,
                )
            },
            mustNotBeBlankTestCases("currency") { value ->
                editExpenseMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryExpense.id!!,
                    currency = value,
                )
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                editExpenseMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryExpense.id!!,
                    notes = value,
                )
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditExpense)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update all fields of an existing expense`() {
            val expense = preconditions {
                expense(
                    workspace = preconditions.fryWorkspace,
                    notes = "Old delivery notes",
                )
            }
            val category = preconditions { category(workspace = preconditions.fryWorkspace) }

            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = expense.id!!,
                        title = "Updated Slurm supplies",
                        datePaid = LocalDate.of(3025, 3, 10),
                        currency = "EUR",
                        originalAmount = 3000,
                        convertedAmountInDefaultCurrency = 3300,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        notes = "Delivery to Omicron Persei 8 - updated",
                        percentOnBusiness = 75,
                        categoryId = category.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditExpense to buildJsonObject {
                        put("id", expense.id!!.toInt())
                        put("title", "Updated Slurm supplies")
                        put("datePaid", "3025-03-10")
                        put("currency", "EUR")
                        put("originalAmount", 3000)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", "Delivery to Omicron Persei 8 - updated")
                        put("percentOnBusiness", 75)
                    }
                )

            aggregateTemplate.findSingle<Expense>(expense.id!!)
                .shouldBeEntityWithFields(
                    Expense(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = category.id,
                        title = "Updated Slurm supplies",
                        datePaid = LocalDate.of(3025, 3, 10),
                        currency = "EUR",
                        originalAmount = 3000,
                        convertedAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 3300,
                            adjustedAmountInDefaultCurrency = 2475,
                        ),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 3300,
                            adjustedAmountInDefaultCurrency = 2475,
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        percentOnBusiness = 75,
                        generalTaxId = null,
                        notes = "Delivery to Omicron Persei 8 - updated",
                        status = ExpenseStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should clear optional notes`() {
            val expense = preconditions {
                expense(
                    workspace = preconditions.fryWorkspace,
                    notes = "Moon cargo delivery",
                )
            }

            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = expense.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditExpense to buildJsonObject {
                        put("id", expense.id!!.toInt())
                        put("title", "Spaceship parts")
                        put("datePaid", MOCK_DATE.toString())
                        put("currency", preconditions.fryWorkspace.defaultCurrency)
                        put("originalAmount", 100)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", JsonNull)
                        put("percentOnBusiness", 100)
                    }
                )

            aggregateTemplate.findSingle<Expense>(expense.id!!)
                .shouldBeEntityWithFields(
                    Expense(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = null,
                        title = "Spaceship parts",
                        datePaid = MOCK_DATE,
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 100,
                        convertedAmounts = AmountsInDefaultCurrency(100, 100),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(100, 100),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        percentOnBusiness = 100,
                        generalTaxId = null,
                        notes = null,
                        status = ExpenseStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should return entity not found error for non-existent expense`() {
            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditExpense)
        }

        @Test
        fun `should return entity not found error for expense in another user workspace`() {
            val zoidbergExpense = preconditions {
                expense(workspace = preconditions.zoidbergWorkspace)
            }

            client
                .graphqlMutation {
                    editExpenseMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergExpense.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditExpense)
        }
    }

    private fun MutationProjection.editExpenseMutation(
        workspaceId: Long,
        id: Long,
        title: String = "Spaceship parts",
        datePaid: LocalDate = MOCK_DATE,
        currency: String = preconditions.fryWorkspace.defaultCurrency,
        originalAmount: Long = 100,
        convertedAmountInDefaultCurrency: Long? = null,
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean = false,
        incomeTaxableAmountInDefaultCurrency: Long? = null,
        notes: String? = null,
        percentOnBusiness: Int? = null,
        attachments: List<Long>? = null,
        categoryId: Long? = null,
        generalTaxId: Long? = null,
    ): MutationProjection = editExpense(
        workspaceId = workspaceId,
        id = id,
        title = title,
        datePaid = datePaid,
        currency = currency,
        originalAmount = originalAmount,
        convertedAmountInDefaultCurrency = convertedAmountInDefaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency = incomeTaxableAmountInDefaultCurrency,
        notes = notes,
        percentOnBusiness = percentOnBusiness,
        attachments = attachments,
        categoryId = categoryId,
        generalTaxId = generalTaxId,
    ) {
        this.id
        this.title
        this.datePaid
        this.currency
        this.originalAmount
        this.useDifferentExchangeRateForIncomeTaxPurposes
        this.notes
        this.percentOnBusiness
    }
}
