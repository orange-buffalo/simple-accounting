package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.web.verifyNotFound
import io.orangebuffalo.accounting.simpleaccounting.web.verifyOkAndJsonBody
import io.orangebuffalo.accounting.simpleaccounting.web.verifyUnauthorized
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
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
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should calculate expenses statistics`(testData: StatisticsApiTestData) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("644")
                inPath("$.finalizedCount").isNumber.isEqualTo("4")
                inPath("$.pendingCount").isNumber.isEqualTo("3")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${testData.firstCategory.id},
                            "totalAmount": 223,
                            "finalizedCount": 2,
                            "pendingCount": 0
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${testData.secondCategory.id},
                            "totalAmount": 421,
                            "finalizedCount": 2,
                            "pendingCount": 3
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
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
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
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
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should calculate incomes statistics`(testData: StatisticsApiTestData) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/incomes" +
                        "?fromDate=3010-04-21&toDate=3010-09-15"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("568")
                inPath("$.finalizedCount").isNumber.isEqualTo("3")
                inPath("$.pendingCount").isNumber.isEqualTo("2")
                inPath("$.currencyExchangeGain").isNumber.isEqualTo("25")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${testData.firstCategory.id},
                            "totalAmount": 335,
                            "finalizedCount": 2,
                            "pendingCount": 0,
                            "currencyExchangeGain":25
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${testData.secondCategory.id},
                            "totalAmount": 233,
                            "finalizedCount": 1,
                            "pendingCount": 2,
                            "currencyExchangeGain": 0
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
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
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
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
                "/api/workspaces/${testData.workspace.id}/statistics/tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should calculate tax payments statistics`(testData: StatisticsApiTestData) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/tax-payments" +
                        "?fromDate=3005-07-02&toDate=3005-08-01"
            )
            .verifyOkAndJsonBody {
                inPath("$.totalTaxPayments").isNumber.isEqualTo("77")
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 if workspace does not exist when requesting tax payments statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/5555/statistics/tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace 5555 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should fail with 404 if workspace belongs to another user when requesting tax payments statistics`(
        testData: StatisticsApiTestData
    ) {
        client.get()
            .uri(
                "/api/workspaces/${testData.workspace.id}/statistics/tax-payments" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .verifyNotFound("Workspace ${testData.workspace.id} is not found")
    }

    class StatisticsApiTestData : TestData {
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
            Prototypes.expense(
                workspace = firstCategory.workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 4, 10),
                reportedAmountInDefaultCurrency = 111,
                actualAmountInDefaultCurrency = 111,
                amountInDefaultCurrency = 111,
                originalAmount = 111
            ),
            Prototypes.expense(
                workspace = firstCategory.workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 4, 9),
                reportedAmountInDefaultCurrency = 110,
                actualAmountInDefaultCurrency = 110,
                amountInDefaultCurrency = 110,
                originalAmount = 110
            ),
            Prototypes.expense(
                workspace = firstCategory.workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 10, 1),
                reportedAmountInDefaultCurrency = 112,
                actualAmountInDefaultCurrency = 112,
                amountInDefaultCurrency = 112,
                originalAmount = 112
            ),
            Prototypes.expense(
                workspace = firstCategory.workspace,
                category = firstCategory,
                datePaid = LocalDate.of(3000, 10, 2),
                reportedAmountInDefaultCurrency = 113,
                actualAmountInDefaultCurrency = 113,
                amountInDefaultCurrency = 113,
                originalAmount = 113
            ),
            Prototypes.expense(
                workspace = secondCategory.workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                reportedAmountInDefaultCurrency = 210,
                actualAmountInDefaultCurrency = 210,
                amountInDefaultCurrency = 210,
                originalAmount = 210
            ),
            Prototypes.expense(
                workspace = secondCategory.workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 7),
                reportedAmountInDefaultCurrency = 211,
                actualAmountInDefaultCurrency = 211,
                amountInDefaultCurrency = 211,
                originalAmount = 211
            ),
            Prototypes.expense(
                workspace = secondCategory.workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                reportedAmountInDefaultCurrency = 0,
                actualAmountInDefaultCurrency = 210,
                amountInDefaultCurrency = 210,
                originalAmount = 210
            ),
            Prototypes.expense(
                workspace = secondCategory.workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                reportedAmountInDefaultCurrency = 0,
                actualAmountInDefaultCurrency = 0,
                amountInDefaultCurrency = 210,
                originalAmount = 210
            ),
            Prototypes.expense(
                workspace = secondCategory.workspace,
                category = secondCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                reportedAmountInDefaultCurrency = 0,
                actualAmountInDefaultCurrency = 0,
                amountInDefaultCurrency = 0,
                originalAmount = 210
            ),
            Prototypes.expense(
                workspace = irrelevantCategory.workspace,
                category = irrelevantCategory,
                datePaid = LocalDate.of(3000, 6, 6),
                reportedAmountInDefaultCurrency = 33,
                actualAmountInDefaultCurrency = 33,
                amountInDefaultCurrency = 33,
                originalAmount = 33
            ),
            Prototypes.income(
                workspace = irrelevantCategory.workspace,
                category = irrelevantCategory,
                dateReceived = LocalDate.of(3010, 5, 23),
                originalAmount = 177,
                amountInDefaultCurrency = 177,
                reportedAmountInDefaultCurrency = 177
            ),
            Prototypes.income(
                workspace = firstCategory.workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 4, 20),
                originalAmount = 166,
                amountInDefaultCurrency = 166,
                reportedAmountInDefaultCurrency = 166
            ),
            Prototypes.income(
                workspace = firstCategory.workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 4, 21),
                originalAmount = 167,
                amountInDefaultCurrency = 185,
                reportedAmountInDefaultCurrency = 167
            ),
            Prototypes.income(
                workspace = firstCategory.workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 9, 15),
                originalAmount = 168,
                amountInDefaultCurrency = 175,
                reportedAmountInDefaultCurrency = 168
            ),
            Prototypes.income(
                workspace = firstCategory.workspace,
                category = firstCategory,
                dateReceived = LocalDate.of(3010, 9, 16),
                originalAmount = 177,
                amountInDefaultCurrency = 177,
                reportedAmountInDefaultCurrency = 177
            ),
            Prototypes.income(
                workspace = secondCategory.workspace,
                category = secondCategory,
                dateReceived = LocalDate.of(3010, 6, 1),
                originalAmount = 233,
                amountInDefaultCurrency = 0,
                reportedAmountInDefaultCurrency = 0
            ),
            Prototypes.income(
                workspace = secondCategory.workspace,
                category = secondCategory,
                dateReceived = LocalDate.of(3010, 6, 1),
                originalAmount = 233,
                amountInDefaultCurrency = 233,
                reportedAmountInDefaultCurrency = 0
            ),
            Prototypes.income(
                workspace = secondCategory.workspace,
                category = secondCategory,
                dateReceived = LocalDate.of(3010, 6, 1),
                originalAmount = 233,
                amountInDefaultCurrency = 233,
                reportedAmountInDefaultCurrency = 233
            ),
            Prototypes.taxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 7, 1),
                amount = 23
            ),
            Prototypes.taxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 7, 2),
                amount = 43
            ),

            Prototypes.taxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 8, 1),
                amount = 34
            ),
            Prototypes.taxPayment(
                workspace = workspace,
                reportingDate = LocalDate.of(3005, 8, 2),
                amount = 111
            )
        )
    }
}