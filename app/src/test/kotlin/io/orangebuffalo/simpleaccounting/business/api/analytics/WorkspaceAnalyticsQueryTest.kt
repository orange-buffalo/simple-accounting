package io.orangebuffalo.simpleaccounting.business.api.analytics

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DisplayName("workspace.analytics")
class WorkspaceAnalyticsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("workspace.analytics.expensesSummary")
    inner class ExpensesSummary {

        @Test
        fun `should calculate expenses summary`() {
            client.graphql {
                workspace(id = preconditions.workspace.id!!) {
                    analytics {
                        expensesSummary(
                            fromDate = LocalDate.of(3000, 4, 10),
                            toDate = LocalDate.of(3000, 10, 1)
                        ) {
                            totalAmount
                            finalizedCount
                            pendingCount
                            currencyExchangeDifference
                            items {
                                category {
                                    id
                                }
                                totalAmount
                                finalizedCount
                                pendingCount
                                currencyExchangeDifference
                            }
                        }
                    }
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("analytics", buildJsonObject {
                            put("expensesSummary", buildJsonObject {
                                put("totalAmount", 12110)
                                put("finalizedCount", 4)
                                put("pendingCount", 3)
                                put("currencyExchangeDifference", -1000)
                                putJsonArray("items") {
                                    addJsonObject {
                                        put("category", buildJsonObject { put("id", preconditions.firstCategory.id!!.toInt()) })
                                        put("totalAmount", 2100)
                                        put("finalizedCount", 2)
                                        put("pendingCount", 0)
                                        put("currencyExchangeDifference", -1000)
                                    }
                                    addJsonObject {
                                        put("category", buildJsonObject { put("id", preconditions.secondCategory.id!!.toInt()) })
                                        put("totalAmount", 10010)
                                        put("finalizedCount", 2)
                                        put("pendingCount", 3)
                                        put("currencyExchangeDifference", 0)
                                    }
                                }
                            })
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("workspace.analytics.incomesSummary")
    inner class IncomesSummary {

        @Test
        fun `should calculate incomes summary`() {
            client.graphql {
                workspace(id = preconditions.workspace.id!!) {
                    analytics {
                        incomesSummary(
                            fromDate = LocalDate.of(3010, 4, 21),
                            toDate = LocalDate.of(3010, 9, 15)
                        ) {
                            totalAmount
                            finalizedCount
                            pendingCount
                            currencyExchangeDifference
                            items {
                                category {
                                    id
                                }
                                totalAmount
                                finalizedCount
                                pendingCount
                                currencyExchangeDifference
                            }
                        }
                    }
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("analytics", buildJsonObject {
                            put("incomesSummary", buildJsonObject {
                                put("totalAmount", 1220)
                                put("finalizedCount", 3)
                                put("pendingCount", 2)
                                put("currencyExchangeDifference", -110)
                                putJsonArray("items") {
                                    addJsonObject {
                                        put("category", buildJsonObject { put("id", preconditions.firstCategory.id!!.toInt()) })
                                        put("totalAmount", 220)
                                        put("finalizedCount", 2)
                                        put("pendingCount", 0)
                                        put("currencyExchangeDifference", -110)
                                    }
                                    addJsonObject {
                                        put("category", buildJsonObject { put("id", preconditions.secondCategory.id!!.toInt()) })
                                        put("totalAmount", 1000)
                                        put("finalizedCount", 1)
                                        put("pendingCount", 2)
                                        put("currencyExchangeDifference", 0)
                                    }
                                }
                            })
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("workspace.analytics.incomeTaxPaymentsSummary")
    inner class IncomeTaxPaymentsSummary {

        @Test
        fun `should calculate income tax payments summary`() {
            client.graphql {
                workspace(id = preconditions.workspace.id!!) {
                    analytics {
                        incomeTaxPaymentsSummary(
                            fromDate = LocalDate.of(3005, 7, 2),
                            toDate = LocalDate.of(3005, 8, 1)
                        ) {
                            totalTaxPayments
                        }
                    }
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("analytics", buildJsonObject {
                            put("incomeTaxPaymentsSummary", buildJsonObject {
                                put("totalTaxPayments", 77)
                            })
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("workspace.analytics.generalTaxesSummary")
    inner class GeneralTaxesSummary {

        @Test
        fun `should calculate general taxes summary`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val vatTax = generalTax(workspace = workspace)
                    val salesTax = generalTax(workspace = workspace)
                }.also {
                    val anotherWorkspace = workspace(owner = it.fry)
                    val anotherTax = generalTax(workspace = anotherWorkspace)

                    // finalized income -> finalizedCollectedTaxes for vatTax
                    income(
                        workspace = it.workspace,
                        generalTax = it.vatTax,
                        generalTaxAmount = 100,
                        convertedAmounts = amountsInDefaultCurrency(900),
                        incomeTaxableAmounts = amountsInDefaultCurrency(800),
                        useDifferentExchangeRateForIncomeTaxPurposes = true,
                        dateReceived = LocalDate.of(3025, 3, 15),
                    )

                    // finalized expense -> finalizedPaidTaxes for salesTax
                    expense(
                        workspace = it.workspace,
                        generalTax = it.salesTax,
                        generalTaxAmount = 50,
                        convertedAmounts = amountsInDefaultCurrency(450),
                        incomeTaxableAmounts = amountsInDefaultCurrency(400),
                        useDifferentExchangeRateForIncomeTaxPurposes = true,
                        datePaid = LocalDate.of(3025, 3, 20),
                        status = ExpenseStatus.FINALIZED,
                    )

                    // pending income -> pendingCollectedTaxes for vatTax
                    income(
                        workspace = it.workspace,
                        generalTax = it.vatTax,
                        convertedAmounts = amountsInDefaultCurrency(256),
                        incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                        useDifferentExchangeRateForIncomeTaxPurposes = true,
                        status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                        dateReceived = LocalDate.of(3025, 3, 18),
                    )

                    // pending expense -> pendingPaidTaxes for salesTax
                    expense(
                        workspace = it.workspace,
                        generalTax = it.salesTax,
                        convertedAmounts = emptyAmountsInDefaultCurrency(),
                        incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        datePaid = LocalDate.of(3025, 3, 10),
                        status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    )

                    // out of date range - excluded
                    income(
                        workspace = it.workspace,
                        generalTax = it.vatTax,
                        generalTaxAmount = 999,
                        convertedAmounts = amountsInDefaultCurrency(999),
                        incomeTaxableAmounts = amountsInDefaultCurrency(999),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        dateReceived = LocalDate.of(3025, 4, 2),
                    )

                    // different workspace - excluded
                    income(
                        workspace = anotherWorkspace,
                        generalTax = anotherTax,
                        generalTaxAmount = 888,
                        convertedAmounts = amountsInDefaultCurrency(888),
                        incomeTaxableAmounts = amountsInDefaultCurrency(888),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        dateReceived = LocalDate.of(3025, 3, 15),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    analytics {
                        generalTaxesSummary(
                            fromDate = LocalDate.of(3025, 3, 1),
                            toDate = LocalDate.of(3025, 4, 1),
                        ) {
                            finalizedCollectedTaxes {
                                tax { id }
                                taxAmount
                                includedItemsNumber
                                includedItemsAmount
                            }
                            finalizedPaidTaxes {
                                tax { id }
                                taxAmount
                                includedItemsNumber
                                includedItemsAmount
                            }
                            pendingCollectedTaxes {
                                tax { id }
                                includedItemsNumber
                            }
                            pendingPaidTaxes {
                                tax { id }
                                includedItemsNumber
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("analytics", buildJsonObject {
                            put("generalTaxesSummary", buildJsonObject {
                                putJsonArray("finalizedCollectedTaxes") {
                                    addJsonObject {
                                        put("tax", buildJsonObject { put("id", testData.vatTax.id!!.toInt()) })
                                        put("taxAmount", 100)
                                        put("includedItemsNumber", 1)
                                        put("includedItemsAmount", 800)
                                    }
                                }
                                putJsonArray("finalizedPaidTaxes") {
                                    addJsonObject {
                                        put("tax", buildJsonObject { put("id", testData.salesTax.id!!.toInt()) })
                                        put("taxAmount", 50)
                                        put("includedItemsNumber", 1)
                                        put("includedItemsAmount", 400)
                                    }
                                }
                                putJsonArray("pendingCollectedTaxes") {
                                    addJsonObject {
                                        put("tax", buildJsonObject { put("id", testData.vatTax.id!!.toInt()) })
                                        put("includedItemsNumber", 1)
                                    }
                                }
                                putJsonArray("pendingPaidTaxes") {
                                    addJsonObject {
                                        put("tax", buildJsonObject { put("id", testData.salesTax.id!!.toInt()) })
                                        put("includedItemsNumber", 1)
                                    }
                                }
                            })
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("workspace.analytics.currenciesShortlist")
    inner class CurrenciesShortlist {

        @Test
        fun `should return currencies sorted by combined usage frequency`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    // 2 expenses in EUR + 1 income in EUR = 3 total for EUR
                    expense(workspace = it.workspace, currency = "EUR")
                    expense(workspace = it.workspace, currency = "EUR")
                    income(workspace = it.workspace, currency = "EUR")
                    // 1 income in CHF
                    income(workspace = it.workspace, currency = "CHF")
                    // 2 expenses in AUD
                    expense(workspace = it.workspace, currency = "AUD")
                    expense(workspace = it.workspace, currency = "AUD")
                    // expenses/incomes from another workspace should not affect the result
                    val anotherWorkspace = workspace(owner = it.fry)
                    expense(workspace = anotherWorkspace, currency = "CHF")
                    expense(workspace = anotherWorkspace, currency = "CHF")
                    expense(workspace = anotherWorkspace, currency = "CHF")
                    income(workspace = anotherWorkspace, currency = "CHF")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    analytics {
                        currenciesShortlist
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("analytics", buildJsonObject {
                            putJsonArray("currenciesShortlist") {
                                // EUR: 3 (2 expenses + 1 income) - highest frequency
                                add("EUR")
                                // AUD: 2 (2 expenses) and CHF: 1 (1 income) - AUD before CHF alphabetically
                                add("AUD")
                                add("CHF")
                            }
                        })
                    }
                )
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val firstCategory = category(workspace = workspace)
            val secondCategory = category(workspace = workspace)

            init {
                val irrelevantWorkspace = workspace(owner = fry)
                val irrelevantCategory = category(workspace = irrelevantWorkspace)

                // Expenses: in range, lower boundary
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 4, 10),
                    originalAmount = 100,
                    convertedAmounts = amountsInDefaultCurrency(100),
                    incomeTaxableAmounts = amountsInDefaultCurrency(100),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
                // Expenses: out of range, -1 day
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 4, 9),
                    originalAmount = 555,
                    convertedAmounts = amountsInDefaultCurrency(555),
                    incomeTaxableAmounts = amountsInDefaultCurrency(555),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
                // Expenses: in range, upper boundary, different exchange rate
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 10, 1),
                    currency = "ZZH",
                    originalAmount = 112,
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 77,
                        adjustedAmountInDefaultCurrency = 1000
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 77,
                        adjustedAmountInDefaultCurrency = 2000
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.FINALIZED
                )
                // Expenses: out of range, +1 day
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 10, 2),
                    originalAmount = 113,
                    convertedAmounts = amountsInDefaultCurrency(113),
                    incomeTaxableAmounts = amountsInDefaultCurrency(113),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
                // Expenses: in range, second category
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    originalAmount = 10,
                    convertedAmounts = amountsInDefaultCurrency(10),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
                // Expenses: in range, second category
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 7),
                    originalAmount = 10000,
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )
                // Expenses: in range, pending conversion for taxation
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    currency = "ZZG",
                    originalAmount = 210,
                    convertedAmounts = amountsInDefaultCurrency(210),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
                )
                // Expenses: in range, pending conversion for taxation
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    currency = "ZZG",
                    originalAmount = 210,
                    convertedAmounts = amountsInDefaultCurrency(210),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
                )
                // Expenses: in range, pending conversion
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    currency = "ZZG",
                    originalAmount = 210,
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.PENDING_CONVERSION
                )
                // Expenses: in range, but out of scope (another workspace)
                expense(
                    workspace = irrelevantWorkspace,
                    category = irrelevantCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    originalAmount = 33,
                    convertedAmounts = amountsInDefaultCurrency(33),
                    incomeTaxableAmounts = amountsInDefaultCurrency(33),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED
                )

                // Incomes: in range, but out of scope (another workspace)
                income(
                    workspace = irrelevantWorkspace,
                    category = irrelevantCategory,
                    dateReceived = LocalDate.of(3010, 5, 23),
                    originalAmount = 177,
                    currency = irrelevantWorkspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(177),
                    incomeTaxableAmounts = amountsInDefaultCurrency(177)
                )
                // Incomes: out of range, -1 day
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 4, 20),
                    originalAmount = 166,
                    currency = workspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(166),
                    incomeTaxableAmounts = amountsInDefaultCurrency(166)
                )
                // Incomes: in range, lower boundary, different exchange rate
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 4, 21),
                    originalAmount = 167,
                    currency = "ZZH",
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 33,
                        adjustedAmountInDefaultCurrency = 10
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 65,
                        adjustedAmountInDefaultCurrency = 20
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
                // Incomes: in range, upper boundary, different exchange rate
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 9, 15),
                    originalAmount = 168,
                    currency = "ZZH",
                    convertedAmounts = amountsInDefaultCurrency(100),
                    incomeTaxableAmounts = amountsInDefaultCurrency(200),
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
                // Incomes: out of range, +1 day
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 9, 16),
                    originalAmount = 177,
                    currency = workspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(177),
                    incomeTaxableAmounts = amountsInDefaultCurrency(177)
                )
                // Incomes: in range, pending conversion
                income(
                    workspace = workspace,
                    category = secondCategory,
                    dateReceived = LocalDate.of(3010, 6, 1),
                    originalAmount = 233,
                    currency = "ZZH",
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION
                )
                // Incomes: in range, pending conversion for taxation
                income(
                    workspace = workspace,
                    category = secondCategory,
                    dateReceived = LocalDate.of(3010, 6, 1),
                    originalAmount = 233,
                    currency = "ZZH",
                    convertedAmounts = amountsInDefaultCurrency(233),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
                // Incomes: in range
                income(
                    workspace = workspace,
                    category = secondCategory,
                    dateReceived = LocalDate.of(3010, 6, 1),
                    originalAmount = 1000,
                    currency = workspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(1000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(1000)
                )

                // Income tax payments
                incomeTaxPayment(workspace = workspace, reportingDate = LocalDate.of(3005, 7, 1), amount = 23)
                incomeTaxPayment(workspace = workspace, reportingDate = LocalDate.of(3005, 7, 2), amount = 43)
                incomeTaxPayment(workspace = workspace, reportingDate = LocalDate.of(3005, 8, 1), amount = 34)
                incomeTaxPayment(workspace = workspace, reportingDate = LocalDate.of(3005, 8, 2), amount = 111)
            }
        }
    }
}
