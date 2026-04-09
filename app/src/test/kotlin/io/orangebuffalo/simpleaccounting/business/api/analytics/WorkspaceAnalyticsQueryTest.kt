package io.orangebuffalo.simpleaccounting.business.api.analytics

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
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
        fun `should return error when accessed anonymously`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
                    analytics {
                        expensesSummary(
                            fromDate = LocalDate.of(3000, 4, 10),
                            toDate = LocalDate.of(3000, 10, 1)
                        ) {
                            totalAmount
                        }
                    }
                }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.Workspace)
        }

        @Test
        fun `should calculate expenses summary`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
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
                                categoryId
                                totalAmount
                                finalizedCount
                                pendingCount
                                currencyExchangeDifference
                            }
                        }
                    }
                }
            }
                .from(preconditions.fry.user)
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
                                        put("categoryId", preconditions.fry.firstCategory.id!!.toInt())
                                        put("totalAmount", 2100)
                                        put("finalizedCount", 2)
                                        put("pendingCount", 0)
                                        put("currencyExchangeDifference", -1000)
                                    }
                                    addJsonObject {
                                        put("categoryId", preconditions.fry.secondCategory.id!!.toInt())
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

        @Test
        fun `should return error when workspace does not exist`() {
            client.graphql {
                workspace(id = -1) {
                    analytics {
                        expensesSummary(
                            fromDate = LocalDate.of(3000, 4, 10),
                            toDate = LocalDate.of(3000, 10, 1)
                        ) {
                            totalAmount
                        }
                    }
                }
            }
                .from(preconditions.fry.user)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.Workspace)
        }

        @Test
        fun `should return error when workspace belongs to another user`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
                    analytics {
                        expensesSummary(
                            fromDate = LocalDate.of(3000, 4, 10),
                            toDate = LocalDate.of(3000, 10, 1)
                        ) {
                            totalAmount
                        }
                    }
                }
            }
                .from(preconditions.zoidberg)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.Workspace)
        }
    }

    @Nested
    @DisplayName("workspace.analytics.incomesSummary")
    inner class IncomesSummary {

        @Test
        fun `should return error when accessed anonymously`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
                    analytics {
                        incomesSummary(
                            fromDate = LocalDate.of(3010, 4, 21),
                            toDate = LocalDate.of(3010, 9, 15)
                        ) {
                            totalAmount
                        }
                    }
                }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.Workspace)
        }

        @Test
        fun `should calculate incomes summary`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
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
                                categoryId
                                totalAmount
                                finalizedCount
                                pendingCount
                                currencyExchangeDifference
                            }
                        }
                    }
                }
            }
                .from(preconditions.fry.user)
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
                                        put("categoryId", preconditions.fry.firstCategory.id!!.toInt())
                                        put("totalAmount", 220)
                                        put("finalizedCount", 2)
                                        put("pendingCount", 0)
                                        put("currencyExchangeDifference", -110)
                                    }
                                    addJsonObject {
                                        put("categoryId", preconditions.fry.secondCategory.id!!.toInt())
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

        @Test
        fun `should return error when workspace does not exist`() {
            client.graphql {
                workspace(id = -1) {
                    analytics {
                        incomesSummary(
                            fromDate = LocalDate.of(3010, 4, 21),
                            toDate = LocalDate.of(3010, 9, 15)
                        ) {
                            totalAmount
                        }
                    }
                }
            }
                .from(preconditions.fry.user)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.Workspace)
        }

        @Test
        fun `should return error when workspace belongs to another user`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
                    analytics {
                        incomesSummary(
                            fromDate = LocalDate.of(3010, 4, 21),
                            toDate = LocalDate.of(3010, 9, 15)
                        ) {
                            totalAmount
                        }
                    }
                }
            }
                .from(preconditions.zoidberg)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.Workspace)
        }
    }

    @Nested
    @DisplayName("workspace.analytics.incomeTaxPaymentsSummary")
    inner class IncomeTaxPaymentsSummary {

        @Test
        fun `should return error when accessed anonymously`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
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
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.Workspace)
        }

        @Test
        fun `should calculate income tax payments summary`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
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
                .from(preconditions.fry.user)
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

        @Test
        fun `should return error when workspace does not exist`() {
            client.graphql {
                workspace(id = -1) {
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
                .from(preconditions.fry.user)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.Workspace)
        }

        @Test
        fun `should return error when workspace belongs to another user`() {
            client.graphql {
                workspace(id = preconditions.fry.workspace.id!!) {
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
                .from(preconditions.zoidberg)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.Workspace)
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val zoidberg = zoidberg().withWorkspace()
            val fry = run {
                val fryUser = fry()
                val fryWorkspace = workspace(owner = fryUser)
                val firstCategory = category(workspace = fryWorkspace)
                val secondCategory = category(workspace = fryWorkspace)
                val irrelevantWorkspace = workspace(owner = fryUser)
                val irrelevantCategory = category(workspace = irrelevantWorkspace)

                // Expenses: in range, lower boundary
                expense(
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 4, 20),
                    originalAmount = 166,
                    currency = fryWorkspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(166),
                    incomeTaxableAmounts = amountsInDefaultCurrency(166)
                )
                // Incomes: in range, lower boundary, different exchange rate
                income(
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 9, 16),
                    originalAmount = 177,
                    currency = fryWorkspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(177),
                    incomeTaxableAmounts = amountsInDefaultCurrency(177)
                )
                // Incomes: in range, pending conversion
                income(
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
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
                    workspace = fryWorkspace,
                    category = secondCategory,
                    dateReceived = LocalDate.of(3010, 6, 1),
                    originalAmount = 1000,
                    currency = fryWorkspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(1000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(1000)
                )

                // Income tax payments
                incomeTaxPayment(workspace = fryWorkspace, reportingDate = LocalDate.of(3005, 7, 1), amount = 23)
                incomeTaxPayment(workspace = fryWorkspace, reportingDate = LocalDate.of(3005, 7, 2), amount = 43)
                incomeTaxPayment(workspace = fryWorkspace, reportingDate = LocalDate.of(3005, 8, 1), amount = 34)
                incomeTaxPayment(workspace = fryWorkspace, reportingDate = LocalDate.of(3005, 8, 2), amount = 111)

                object {
                    val user = fryUser
                    val workspace = fryWorkspace
                    val firstCategory = firstCategory
                    val secondCategory = secondCategory
                }
            }
        }
    }
}
