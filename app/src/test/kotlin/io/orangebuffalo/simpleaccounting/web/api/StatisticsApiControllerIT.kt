package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeStatus
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@SimpleAccountingIntegrationTest
@DisplayName("Statistics API ")
internal class StatisticsApiControllerIT(
    @Autowired private val client: WebTestClient,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    fun `should allow access to expenses statistics only for authenticated users`() {
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate expenses statistics`() {
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("12110")
                inPath("$.finalizedCount").isNumber.isEqualTo("4")
                inPath("$.pendingCount").isNumber.isEqualTo("3")
                inPath("$.currencyExchangeDifference").isNumber.isEqualTo("-1000")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${testData.firstCategory.id},
                            "totalAmount": 2100,
                            "finalizedCount": 2,
                            "pendingCount": 0,
                            "currencyExchangeDifference": -1000
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${testData.secondCategory.id},
                            "totalAmount": 10010,
                            "finalizedCount": 2,
                            "pendingCount": 3,
                            "currencyExchangeDifference": 0
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 if workspace does not exist when requesting expenses statistics`() {
        setupPreconditions()
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
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    @Test
    fun `should allow access to incomes statistics only for authenticated users`() {
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate incomes statistics`() {
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/incomes" +
                        "?fromDate=3010-04-21&toDate=3010-09-15"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("1220")
                inPath("$.finalizedCount").isNumber.isEqualTo("3")
                inPath("$.pendingCount").isNumber.isEqualTo("2")
                inPath("$.currencyExchangeDifference").isNumber.isEqualTo("-110")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${testData.firstCategory.id},
                            "totalAmount": 220,
                            "finalizedCount": 2,
                            "pendingCount": 0,
                            "currencyExchangeDifference": -110
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${testData.secondCategory.id},
                            "totalAmount": 1000,
                            "finalizedCount": 1,
                            "pendingCount": 2,
                            "currencyExchangeDifference": 0
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 if workspace does not exist when requesting incomes statistics`() {
        setupPreconditions()
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
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    @Test
    fun `should allow access to tax payments statistics only for authenticated users`() {
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax payments statistics`() {
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3005-07-02&toDate=3005-08-01"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalTaxPayments").isNumber.isEqualTo("77")
            }
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 if workspace does not exist when requesting tax payments statistics`() {
        setupPreconditions()
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
        val testData = setupPreconditions()
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
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
