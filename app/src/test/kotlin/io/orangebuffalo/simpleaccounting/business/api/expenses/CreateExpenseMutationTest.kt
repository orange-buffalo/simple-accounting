package io.orangebuffalo.simpleaccounting.business.api.expenses

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.Expense
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseAttachment
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
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
import java.time.LocalDate

@DisplayName("createExpense mutation")
class CreateExpenseMutationTest(
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
                .graphqlMutation { createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateExpense)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateExpense)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateExpense)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            mustNotBeBlankTestCases("currency") { value ->
                createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!, currency = value)
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                createExpenseMutation(workspaceId = preconditions.fryWorkspace.id!!, notes = value)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateExpense)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create an expense with all fields`() {
            val document = preconditions { document(workspace = preconditions.fryWorkspace) }
            val category = preconditions { category(workspace = preconditions.fryWorkspace) }
            val generalTax = preconditions { generalTax(workspace = preconditions.fryWorkspace, rateInBps = 1000) }

            client
                .graphqlMutation {
                    createExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Slurm supplies",
                        datePaid = LocalDate.of(3025, 1, 15),
                        currency = "EUR",
                        originalAmount = 5000,
                        convertedAmountInDefaultCurrency = 4500,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        notes = "Good news, everyone! Slurm delivery complete",
                        percentOnBusiness = 80,
                        attachments = listOf(document.id!!),
                        categoryId = category.id!!,
                        generalTaxId = generalTax.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateExpense to buildJsonObject {
                        put("title", "Slurm supplies")
                        put("datePaid", "3025-01-15")
                        put("currency", "EUR")
                        put("originalAmount", 5000)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", "Good news, everyone! Slurm delivery complete")
                        put("percentOnBusiness", 80)
                    }
                )

            aggregateTemplate.findAll<Expense>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Slurm supplies" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Expense(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = category.id,
                        title = "Slurm supplies",
                        datePaid = LocalDate.of(3025, 1, 15),
                        currency = "EUR",
                        originalAmount = 5000,
                        convertedAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 4500,
                            adjustedAmountInDefaultCurrency = 3273,
                        ),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 4500,
                            adjustedAmountInDefaultCurrency = 3273,
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        percentOnBusiness = 80,
                        attachments = setOf(ExpenseAttachment(document.id!!)),
                        generalTaxId = generalTax.id,
                        generalTaxRateInBps = 1000,
                        generalTaxAmount = 327,
                        notes = "Good news, everyone! Slurm delivery complete",
                        status = ExpenseStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should create an expense without optional fields`() {
            client
                .graphqlMutation {
                    createExpenseMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Robot oil",
                        datePaid = LocalDate.of(3025, 2, 1),
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 200,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateExpense to buildJsonObject {
                        put("title", "Robot oil")
                        put("datePaid", "3025-02-01")
                        put("currency", preconditions.fryWorkspace.defaultCurrency)
                        put("originalAmount", 200)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", JsonNull)
                        put("percentOnBusiness", 100)
                    }
                )

            aggregateTemplate.findAll<Expense>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Robot oil" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Expense(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = null,
                        title = "Robot oil",
                        datePaid = LocalDate.of(3025, 2, 1),
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 200,
                        convertedAmounts = AmountsInDefaultCurrency(200, 200),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(200, 200),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        percentOnBusiness = 100,
                        generalTaxId = null,
                        notes = null,
                        status = ExpenseStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createExpenseMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateExpense)
        }
    }

    private fun MutationProjection.createExpenseMutation(
        workspaceId: Long,
        title: String = "Spaceship parts",
        datePaid: LocalDate = MOCK_DATE,
        currency: String = "USD",
        originalAmount: Long = 100,
        convertedAmountInDefaultCurrency: Long? = null,
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean = false,
        incomeTaxableAmountInDefaultCurrency: Long? = null,
        notes: String? = null,
        percentOnBusiness: Int? = null,
        attachments: List<Long>? = null,
        categoryId: Long? = null,
        generalTaxId: Long? = null,
    ): MutationProjection = createExpense(
        workspaceId = workspaceId,
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
        this.title
        this.datePaid
        this.currency
        this.originalAmount
        this.useDifferentExchangeRateForIncomeTaxPurposes
        this.notes
        this.percentOnBusiness
    }
}
