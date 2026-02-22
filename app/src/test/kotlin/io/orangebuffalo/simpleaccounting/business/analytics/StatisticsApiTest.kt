package io.orangebuffalo.simpleaccounting.business.analytics

import io.kotest.matchers.equals.shouldBeEqual
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeJsonInteger
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.javacrumbs.jsonunit.kotest.inPath
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@DisplayName("Statistics API ")
internal class StatisticsApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow access to expenses statistics only for authenticated users`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate expenses statistics`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyOkAndJsonBodyEqualTo {
                put("totalAmount", 12110)
                put("finalizedCount", 4)
                put("pendingCount", 3)
                put("currencyExchangeDifference", -1000)
                putJsonArray("items") {
                    addJsonObject {
                        put("categoryId", preconditions.firstCategory.id)
                        put("totalAmount", 2100)
                        put("finalizedCount", 2)
                        put("pendingCount", 0)
                        put("currencyExchangeDifference", -1000)
                    }
                    addJsonObject {
                        put("categoryId", preconditions.secondCategory.id)
                        put("totalAmount", 10010)
                        put("finalizedCount", 2)
                        put("pendingCount", 3)
                        put("currencyExchangeDifference", 0)
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 if workspace does not exist when requesting expenses statistics`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 if workspace belongs to another user when requesting expenses statistics`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${preconditions.workspace.id} is not found")
    }

    @Test
    fun `should allow access to incomes statistics only for authenticated users`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate incomes statistics`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/incomes" +
                        "?fromDate=3010-04-21&toDate=3010-09-15"
            )
            .verifyOkAndJsonBodyEqualTo {
                put("totalAmount", 1220)
                put("finalizedCount", 3)
                put("pendingCount", 2)
                put("currencyExchangeDifference", -110)
                putJsonArray("items") {
                    addJsonObject {
                        put("categoryId", preconditions.firstCategory.id)
                        put("totalAmount", 220)
                        put("finalizedCount", 2)
                        put("pendingCount", 0)
                        put("currencyExchangeDifference", -110)
                    }
                    addJsonObject {
                        put("categoryId", preconditions.secondCategory.id)
                        put("totalAmount", 1000)
                        put("finalizedCount", 1)
                        put("pendingCount", 2)
                        put("currencyExchangeDifference", 0)
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 if workspace does not exist when requesting incomes statistics`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 if workspace belongs to another user when requesting incomes statistics`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${preconditions.workspace.id} is not found")
    }

    @Test
    fun `should allow access to tax payments statistics only for authenticated users`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax payments statistics`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3005-07-02&toDate=3005-08-01"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalTaxPayments").shouldBeJsonInteger().shouldBeEqual(77)
            }
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 if workspace does not exist when requesting tax payments statistics`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 if workspace belongs to another user when requesting tax payments statistics`() {
        client.get()
            .uri(
                "/api/workspaces/${preconditions.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${preconditions.workspace.id} is not found")
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val workspace = workspace(owner = fry)
            val irrelevantWorkspace = workspace(owner = fry)
            val firstCategory = category(workspace = workspace)
            val secondCategory = category(workspace = workspace)
            val irrelevantCategory = category(workspace = irrelevantWorkspace)

            init {
                category(workspace = workspace)
                // in range, lower boundary
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
                // out of range: -1 day
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 4, 9),
                    convertedAmounts = amountsInDefaultCurrency(555),
                    incomeTaxableAmounts = amountsInDefaultCurrency(555),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    originalAmount = 555
                )
                // in range, upper boundary
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 10, 1),
                    currency = "ZZH",
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 77,
                        // must consider only adjusted amounts
                        adjustedAmountInDefaultCurrency = 1000
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 77,
                        // must consider only adjusted amounts
                        adjustedAmountInDefaultCurrency = 2000
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.FINALIZED,
                    originalAmount = 112
                )
                // out of range: +1 day
                expense(
                    workspace = workspace,
                    category = firstCategory,
                    datePaid = LocalDate.of(3000, 10, 2),
                    convertedAmounts = amountsInDefaultCurrency(113),
                    incomeTaxableAmounts = amountsInDefaultCurrency(113),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    originalAmount = 113
                )
                // in range
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    convertedAmounts = amountsInDefaultCurrency(10),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    originalAmount = 10
                )
                // in range
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 7),
                    convertedAmounts = amountsInDefaultCurrency(10000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(10000),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    originalAmount = 10000
                )
                // in range: pending
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    currency = "ZZG",
                    convertedAmounts = amountsInDefaultCurrency(210),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    originalAmount = 210
                )
                // in range: pending
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    currency = "ZZG",
                    convertedAmounts = amountsInDefaultCurrency(210),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = true,
                    status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                    originalAmount = 210
                )
                // in range: pending
                expense(
                    workspace = workspace,
                    category = secondCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    currency = "ZZG",
                    convertedAmounts = emptyAmountsInDefaultCurrency(),
                    incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.PENDING_CONVERSION,
                    originalAmount = 210
                )
                // in range, but out of scope: another workspace
                expense(
                    workspace = irrelevantWorkspace,
                    category = irrelevantCategory,
                    datePaid = LocalDate.of(3000, 6, 6),
                    convertedAmounts = amountsInDefaultCurrency(33),
                    incomeTaxableAmounts = amountsInDefaultCurrency(33),
                    useDifferentExchangeRateForIncomeTaxPurposes = false,
                    status = ExpenseStatus.FINALIZED,
                    originalAmount = 33
                )

                // in range, but out of scope: another workspace
                income(
                    workspace = irrelevantWorkspace,
                    category = irrelevantCategory,
                    dateReceived = LocalDate.of(3010, 5, 23),
                    originalAmount = 177,
                    currency = irrelevantWorkspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(177),
                    incomeTaxableAmounts = amountsInDefaultCurrency(177)
                )
                // out of range: -1 day
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 4, 20),
                    originalAmount = 166,
                    currency = workspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(166),
                    incomeTaxableAmounts = amountsInDefaultCurrency(166)
                )
                // in range: lower boundary
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 4, 21),
                    originalAmount = 167,
                    currency = "ZZH",
                    convertedAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 33,
                        // must consider only adjusted amounts
                        adjustedAmountInDefaultCurrency = 10
                    ),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(
                        originalAmountInDefaultCurrency = 65,
                        // must consider only adjusted amounts
                        adjustedAmountInDefaultCurrency = 20
                    ),
                    useDifferentExchangeRateForIncomeTaxPurposes = true
                )
                // in range: upper boundary
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
                // out of rage: +1 day
                income(
                    workspace = workspace,
                    category = firstCategory,
                    dateReceived = LocalDate.of(3010, 9, 16),
                    originalAmount = 177,
                    currency = workspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(177),
                    incomeTaxableAmounts = amountsInDefaultCurrency(177)
                )
                // in range, pending
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
                // in range: pending
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
                // in range
                income(
                    workspace = workspace,
                    category = secondCategory,
                    dateReceived = LocalDate.of(3010, 6, 1),
                    originalAmount = 1000,
                    currency = workspace.defaultCurrency,
                    convertedAmounts = amountsInDefaultCurrency(1000),
                    incomeTaxableAmounts = amountsInDefaultCurrency(1000)
                )

                incomeTaxPayment(
                    workspace = workspace,
                    reportingDate = LocalDate.of(3005, 7, 1),
                    amount = 23
                )
                incomeTaxPayment(
                    workspace = workspace,
                    reportingDate = LocalDate.of(3005, 7, 2),
                    amount = 43
                )

                incomeTaxPayment(
                    workspace = workspace,
                    reportingDate = LocalDate.of(3005, 8, 1),
                    amount = 34
                )
                incomeTaxPayment(
                    workspace = workspace,
                    reportingDate = LocalDate.of(3005, 8, 2),
                    amount = 111
                )
            }
        }
    }
}
