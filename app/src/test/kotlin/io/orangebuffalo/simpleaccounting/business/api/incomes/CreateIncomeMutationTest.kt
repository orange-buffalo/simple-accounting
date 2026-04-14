package io.orangebuffalo.simpleaccounting.business.api.incomes

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
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

@DisplayName("createIncome mutation")
class CreateIncomeMutationTest(
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
                .graphqlMutation { createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateIncome)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateIncome)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateIncome)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            mustNotBeBlankTestCases("currency") { value ->
                createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, currency = value)
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                createIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, notes = value)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateIncome)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create an income with all fields`() {
            val document = preconditions { document(workspace = preconditions.fryWorkspace) }
            val category = preconditions { category(workspace = preconditions.fryWorkspace) }
            val generalTax = preconditions { generalTax(workspace = preconditions.fryWorkspace, rateInBps = 2000) }

            client
                .graphqlMutation {
                    createIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Planet Express delivery fee",
                        dateReceived = LocalDate.of(3025, 1, 15),
                        currency = "EUR",
                        originalAmount = 12345L,
                        convertedAmountInDefaultCurrency = 10000L,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        notes = "Good news, everyone! Delivery payment received.",
                        attachments = listOf(document.id!!),
                        categoryId = category.id!!,
                        generalTaxId = generalTax.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateIncome to buildJsonObject {
                        put("title", "Planet Express delivery fee")
                        put("dateReceived", "3025-01-15")
                        put("currency", "EUR")
                        put("originalAmount", 12345L)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", "Good news, everyone! Delivery payment received.")
                    }
                )

            aggregateTemplate.findAll<Income>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Planet Express delivery fee" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Income(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = category.id,
                        title = "Planet Express delivery fee",
                        dateReceived = LocalDate.of(3025, 1, 15),
                        currency = "EUR",
                        originalAmount = 12345L,
                        convertedAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 10000L,
                            adjustedAmountInDefaultCurrency = 8333L,
                        ),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 10000L,
                            adjustedAmountInDefaultCurrency = 8333L,
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        attachments = setOf(IncomeAttachment(document.id!!)),
                        generalTaxId = generalTax.id,
                        generalTaxRateInBps = 2000,
                        generalTaxAmount = 1667L,
                        notes = "Good news, everyone! Delivery payment received.",
                        status = IncomeStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should create an income without optional fields`() {
            client
                .graphqlMutation {
                    createIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Slurm delivery payment",
                        dateReceived = LocalDate.of(3025, 2, 1),
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 5000L,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateIncome to buildJsonObject {
                        put("title", "Slurm delivery payment")
                        put("dateReceived", "3025-02-01")
                        put("currency", preconditions.fryWorkspace.defaultCurrency)
                        put("originalAmount", 5000L)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", JsonNull)
                    }
                )

            aggregateTemplate.findAll<Income>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Slurm delivery payment" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Income(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = null,
                        title = "Slurm delivery payment",
                        dateReceived = LocalDate.of(3025, 2, 1),
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 5000L,
                        convertedAmounts = AmountsInDefaultCurrency(5000L, 5000L),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(5000L, 5000L),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        generalTaxId = null,
                        notes = null,
                        status = IncomeStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should create an income with linked invoice`() {
            val customer = preconditions { customer(workspace = preconditions.fryWorkspace, name = "MomCorp") }
            val linkedInvoice = preconditions { invoice(customer = customer, title = "Robot oil invoice") }

            client
                .graphqlMutation {
                    createIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Robot oil payment",
                        dateReceived = LocalDate.of(3025, 3, 10),
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 1500L,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        linkedInvoiceId = linkedInvoice.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateIncome to buildJsonObject {
                        put("title", "Robot oil payment")
                        put("dateReceived", "3025-03-10")
                        put("currency", preconditions.fryWorkspace.defaultCurrency)
                        put("originalAmount", 1500L)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", JsonNull)
                    }
                )

            aggregateTemplate.findAll<Income>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Robot oil payment" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Income(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = null,
                        title = "Robot oil payment",
                        dateReceived = LocalDate.of(3025, 3, 10),
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 1500L,
                        convertedAmounts = AmountsInDefaultCurrency(1500L, 1500L),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(1500L, 1500L),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        linkedInvoiceId = linkedInvoice.id,
                        generalTaxId = null,
                        status = IncomeStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createIncomeMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateIncome)
        }
    }

    private fun MutationProjection.createIncomeMutation(
        workspaceId: Long,
        title: String = "Interplanetary cargo income",
        dateReceived: LocalDate = MOCK_DATE,
        currency: String = "USD",
        originalAmount: Long = 100L,
        convertedAmountInDefaultCurrency: Long? = null,
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean = false,
        incomeTaxableAmountInDefaultCurrency: Long? = null,
        notes: String? = null,
        attachments: List<Long>? = null,
        categoryId: Long? = null,
        generalTaxId: Long? = null,
        linkedInvoiceId: Long? = null,
    ): MutationProjection = createIncome(
        workspaceId = workspaceId,
        title = title,
        dateReceived = dateReceived,
        currency = currency,
        originalAmount = originalAmount,
        convertedAmountInDefaultCurrency = convertedAmountInDefaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency = incomeTaxableAmountInDefaultCurrency,
        notes = notes,
        attachments = attachments,
        categoryId = categoryId,
        generalTaxId = generalTaxId,
        linkedInvoiceId = linkedInvoiceId,
    ) {
        this.title
        this.dateReceived
        this.currency
        this.originalAmount
        this.useDifferentExchangeRateForIncomeTaxPurposes
        this.notes
    }
}
