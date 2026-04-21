package io.orangebuffalo.simpleaccounting.business.api.incomes

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
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

@DisplayName("editIncome mutation")
class EditIncomeMutationTest(
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
            val fryIncome = income(workspace = fryWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = preconditions.fryIncome.id!!,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditIncome)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = preconditions.fryIncome.id!!,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditIncome)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = preconditions.fryIncome.id!!,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditIncome)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                editIncomeMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryIncome.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                editIncomeMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryIncome.id!!,
                    title = value,
                )
            },
            mustNotBeBlankTestCases("currency") { value ->
                editIncomeMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryIncome.id!!,
                    currency = value,
                )
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                editIncomeMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryIncome.id!!,
                    notes = value,
                )
            },
            requiredFieldRejectedTestCases("id") {
                editIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryIncome.id!!)
            },
            requiredFieldRejectedTestCases("dateReceived") {
                editIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryIncome.id!!)
            },
            requiredFieldRejectedTestCases("originalAmount") {
                editIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryIncome.id!!)
            },
            requiredFieldRejectedTestCases("useDifferentExchangeRateForIncomeTaxPurposes") {
                editIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryIncome.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                editIncomeMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryIncome.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditIncome)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update all fields of an existing income`() {
            val income = preconditions {
                income(
                    workspace = preconditions.fryWorkspace,
                    notes = "Old delivery notes",
                )
            }
            val category = preconditions { category(workspace = preconditions.fryWorkspace) }
            val generalTax = preconditions { generalTax(workspace = preconditions.fryWorkspace, rateInBps = 1500) }

            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = income.id!!,
                        title = "Updated Planet Express delivery",
                        dateReceived = LocalDate.of(3025, 3, 10),
                        currency = "EUR",
                        originalAmount = 8000L,
                        convertedAmountInDefaultCurrency = 9000L,
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        notes = "Good news, everyone! Updated delivery to Omicron Persei 8.",
                        categoryId = category.id!!,
                        generalTaxId = generalTax.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditIncome to buildJsonObject {
                        put("id", income.id!!.toInt())
                        put("title", "Updated Planet Express delivery")
                        put("dateReceived", "3025-03-10")
                        put("currency", "EUR")
                        put("originalAmount", 8000L)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", "Good news, everyone! Updated delivery to Omicron Persei 8.")
                    }
                )

            aggregateTemplate.findSingle<Income>(income.id!!)
                .shouldBeEntityWithFields(
                    Income(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = category.id,
                        title = "Updated Planet Express delivery",
                        dateReceived = LocalDate.of(3025, 3, 10),
                        currency = "EUR",
                        originalAmount = 8000L,
                        convertedAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 9000L,
                            adjustedAmountInDefaultCurrency = 7826L,
                        ),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(
                            originalAmountInDefaultCurrency = 9000L,
                            adjustedAmountInDefaultCurrency = 7826L,
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        generalTaxId = generalTax.id,
                        generalTaxRateInBps = 1500,
                        generalTaxAmount = 1174L,
                        notes = "Good news, everyone! Updated delivery to Omicron Persei 8.",
                        status = IncomeStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should clear optional notes`() {
            val income = preconditions {
                income(
                    workspace = preconditions.fryWorkspace,
                    notes = "Moon cargo delivery",
                )
            }

            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = income.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditIncome to buildJsonObject {
                        put("id", income.id!!.toInt())
                        put("title", "Interplanetary cargo income")
                        put("dateReceived", MOCK_DATE.toString())
                        put("currency", preconditions.fryWorkspace.defaultCurrency)
                        put("originalAmount", 100L)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", JsonNull)
                    }
                )

            aggregateTemplate.findSingle<Income>(income.id!!)
                .shouldBeEntityWithFields(
                    Income(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = null,
                        title = "Interplanetary cargo income",
                        dateReceived = MOCK_DATE,
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 100L,
                        convertedAmounts = AmountsInDefaultCurrency(100L, 100L),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(100L, 100L),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        generalTaxId = null,
                        notes = null,
                        status = IncomeStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should set and clear linkedInvoice`() {
            val income = preconditions {
                income(workspace = preconditions.fryWorkspace, title = "Slurm payment")
            }
            val fryCustomer = preconditions { customer(workspace = preconditions.fryWorkspace, name = "MomCorp") }
            val linkedInvoice = preconditions { invoice(customer = fryCustomer, title = "Slurm invoice") }

            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = income.id!!,
                        linkedInvoiceId = linkedInvoice.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditIncome to buildJsonObject {
                        put("id", income.id!!.toInt())
                        put("title", "Interplanetary cargo income")
                        put("dateReceived", MOCK_DATE.toString())
                        put("currency", preconditions.fryWorkspace.defaultCurrency)
                        put("originalAmount", 100L)
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        put("notes", JsonNull)
                    }
                )

            aggregateTemplate.findSingle<Income>(income.id!!)
                .shouldBeEntityWithFields(
                    Income(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        categoryId = null,
                        title = "Interplanetary cargo income",
                        dateReceived = MOCK_DATE,
                        currency = preconditions.fryWorkspace.defaultCurrency,
                        originalAmount = 100L,
                        convertedAmounts = AmountsInDefaultCurrency(100L, 100L),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(100L, 100L),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        linkedInvoiceId = linkedInvoice.id,
                        generalTaxId = null,
                        status = IncomeStatus.FINALIZED,
                    )
                )
        }

        @Test
        fun `should return entity not found error for non-existent income`() {
            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditIncome)
        }

        @Test
        fun `should return entity not found error for income in another user workspace`() {
            val zoidbergIncome = preconditions {
                income(workspace = preconditions.zoidbergWorkspace)
            }

            client
                .graphqlMutation {
                    editIncomeMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergIncome.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditIncome)
        }
    }

    private fun MutationProjection.editIncomeMutation(
        workspaceId: Long,
        id: Long,
        title: String = "Interplanetary cargo income",
        dateReceived: LocalDate = MOCK_DATE,
        currency: String = preconditions.fryWorkspace.defaultCurrency,
        originalAmount: Long = 100L,
        convertedAmountInDefaultCurrency: Long? = null,
        useDifferentExchangeRateForIncomeTaxPurposes: Boolean = false,
        incomeTaxableAmountInDefaultCurrency: Long? = null,
        notes: String? = null,
        attachments: List<Long>? = null,
        categoryId: Long? = null,
        generalTaxId: Long? = null,
        linkedInvoiceId: Long? = null,
    ): MutationProjection = editIncome(
        workspaceId = workspaceId,
        id = id,
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
        this.id
        this.title
        this.dateReceived
        this.currency
        this.originalAmount
        this.useDifferentExchangeRateForIncomeTaxPurposes
        this.notes
    }
}
