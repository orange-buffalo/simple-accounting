package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeStatus
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@SimpleAccountingIntegrationTest
@DisplayName("Statistics API ")
internal class StatisticsApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should allow access to expenses statistics only for authenticated users`(testData: StatisticsApiTestData) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate expenses statistics`(testData: StatisticsApiTestData) {
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
    fun `should fail with 404 if workspace does not exist when requesting expenses statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 if workspace belongs to another user when requesting expenses statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    @Test
    fun `should allow access to incomes statistics only for authenticated users`(testData: StatisticsApiTestData) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate incomes statistics`(testData: StatisticsApiTestData) {
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
    fun `should fail with 404 if workspace does not exist when requesting incomes statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 if workspace belongs to another user when requesting incomes statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/incomes" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    @Test
    fun `should allow access to tax payments statistics only for authenticated users`(testData: StatisticsApiTestData) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should calculate tax payments statistics`(testData: StatisticsApiTestData) {
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
    fun `should fail with 404 if workspace does not exist when requesting tax payments statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 if workspace belongs to another user when requesting tax payments statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/income-tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    class StatisticsApiTestData : TestDataDeprecated {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val workspace = Prototypes.workspace(owner = fry)
        val irrelevantWorkspace = Prototypes.workspace(owner = fry)
        val firstCategory = Prototypes.category(workspace = workspace)
        val secondCategory = Prototypes.category(workspace = workspace)
        val thirdCategory = Prototypes.category(workspace = workspace)
        val irrelevantCategory = Prototypes.category(workspace = irrelevantWorkspace)

        override fun generateData() = listOf(
            farnsworth, fry, workspace, irrelevantWorkspace,
            firstCategory, secondCategory, thirdCategory, irrelevantCategory,

            // in range, lower boundary
            Prototypes.expense(
                workspace = workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 4, 10),
                originalAmount = 100,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(100),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(100),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.FINALIZED
            ),
            // out of range: -1 day
            Prototypes.expense(
                workspace = workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 4, 9),
                convertedAmounts = Prototypes.amountsInDefaultCurrency(555),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(555),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.FINALIZED,
                originalAmount = 555
            ),
            // in range, upper boundary
            Prototypes.expense(
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
            ),
            // out of range: +1 day
            Prototypes.expense(
                workspace = workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 10, 2),
                convertedAmounts = Prototypes.amountsInDefaultCurrency(113),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(113),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.FINALIZED,
                originalAmount = 113
            ),
            // in range
            Prototypes.expense(
                workspace = workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                convertedAmounts = Prototypes.amountsInDefaultCurrency(10),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(10),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.FINALIZED,
                originalAmount = 10
            ),
            // in range
            Prototypes.expense(
                workspace = workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 7),
                convertedAmounts = Prototypes.amountsInDefaultCurrency(10000),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(10000),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.FINALIZED,
                originalAmount = 10000
            ),
            // in range: pending
            Prototypes.expense(
                workspace = workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                currency = "ZZG",
                convertedAmounts = Prototypes.amountsInDefaultCurrency(210),
                incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                originalAmount = 210
            ),
            // in range: pending
            Prototypes.expense(
                workspace = workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                currency = "ZZG",
                convertedAmounts = Prototypes.amountsInDefaultCurrency(210),
                incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                originalAmount = 210
            ),
            // in range: pending
            Prototypes.expense(
                workspace = workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                currency = "ZZG",
                convertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.PENDING_CONVERSION,
                originalAmount = 210
            ),
            // in range, but out of scope: another workspace
            Prototypes.expense(
                workspace = irrelevantWorkspace,
                category = irrelevantCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                convertedAmounts = Prototypes.amountsInDefaultCurrency(33),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(33),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                status = ExpenseStatus.FINALIZED,
                originalAmount = 33
            ),

            // in range, but out of scope: another workspace
            Prototypes.income(
                workspace = irrelevantWorkspace,
                category = irrelevantCategory,
                dateReceived = LocalDate.of(3010, 5, 23),
                originalAmount = 177,
                currency = irrelevantWorkspace.defaultCurrency,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(177),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(177)
            ),
            // out of range: -1 day
            Prototypes.income(
                workspace = workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 4, 20),
                originalAmount = 166,
                currency = workspace.defaultCurrency,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(166),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(166)
            ),
            // in range: lower boundary
            Prototypes.income(
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
            ),
            // in range: upper boundary
            Prototypes.income(
                workspace = workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 9, 15),
                originalAmount = 168,
                currency = "ZZH",
                convertedAmounts = Prototypes.amountsInDefaultCurrency(100),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(200),
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            // out of rage: +1 day
            Prototypes.income(
                workspace = workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 9, 16),
                originalAmount = 177,
                currency = workspace.defaultCurrency,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(177),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(177)
            ),
            // in range, pending
            Prototypes.income(
                workspace = workspace,
                category = secondCategory,
                dateReceived = LocalDate.of(3010, 6, 1),
                originalAmount = 233,
                currency = "ZZH",
                convertedAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                status = IncomeStatus.PENDING_CONVERSION
            ),
            // in range: pending
            Prototypes.income(
                workspace = workspace,
                category = secondCategory,
                dateReceived = LocalDate.of(3010, 6, 1),
                originalAmount = 233,
                currency = "ZZH",
                convertedAmounts = Prototypes.amountsInDefaultCurrency(233),
                incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency(),
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
                useDifferentExchangeRateForIncomeTaxPurposes = true
            ),
            // in range
            Prototypes.income(
                workspace = workspace,
                category = secondCategory,
                dateReceived = LocalDate.of(3010, 6, 1),
                originalAmount = 1000,
                currency = workspace.defaultCurrency,
                convertedAmounts = Prototypes.amountsInDefaultCurrency(1000),
                incomeTaxableAmounts = Prototypes.amountsInDefaultCurrency(1000)
            ),

            Prototypes.incomeTaxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 7, 1),
                amount = 23
            ),
            Prototypes.incomeTaxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 7, 2),
                amount = 43
            ),

            Prototypes.incomeTaxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 8, 1),
                amount = 34
            ),
            Prototypes.incomeTaxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 8, 2),
                amount = 111
            )
        )
    }
}
