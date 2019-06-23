package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.accounting.simpleaccounting.web.*
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Expenses API ")
internal class ExpensesApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val dbHelper: DbHelper,
    @Autowired val expenseRepository: ExpenseRepository
) {

    @MockBean
    lateinit var timeService: TimeService

    @Test
    fun `should allow GET access only for logged in users`(testData: ExpensesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return expenses of a workspace of current user`(testData: ExpensesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "best ever slurm",
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            actualAmountInDefaultCurrency: 450,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            percentOnBusiness: 100,
                            id: ${testData.firstSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    ),

                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "another great slurm",
                            currency: "ZZB",
                            originalAmount: 5100,
                            amountInDefaultCurrency: 510,
                            actualAmountInDefaultCurrency: 460,
                            reportedAmountInDefaultCurrency: 455,
                            attachments: [${testData.slurmReceipt.id}],
                            notes: "nice!",
                            percentOnBusiness: 99,
                            id: ${testData.secondSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found on GET`(testData: ExpensesApiTestData) {
        client.get()
            .uri("/api/workspaces/27347947239/expenses")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 on GET if workspace belongs to another user`(testData: ExpensesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return expense by id for current user`(testData: ExpensesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "best ever slurm",
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            actualAmountInDefaultCurrency: 450,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            percentOnBusiness: 100,
                            id: ${testData.firstSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when requesting expense by id`(testData: ExpensesApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/expenses/${testData.firstSlurm.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 if workspace belongs to another user when requesting expense by id`(
        testData: ExpensesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if expense belongs to another workspace when requesting expense by id`(
        testData: ExpensesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.coffeeExpense.id}")
            .verifyNotFound("Expense ${testData.coffeeExpense.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when creating expense`(testData: ExpensesApiTestData) {
        client.post()
            .uri("/api/workspaces/995943/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(testData.defaultNewExpense())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new expense`(testData: ExpensesApiTestData) {
        val expenseId = dbHelper.getNextId()
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [${testData.slurmReceipt.id}],
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "tax": ${testData.slurmTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "ever best drink",
                            currency: "AUD",
                            originalAmount: 30000,
                            amountInDefaultCurrency: 42000,
                            actualAmountInDefaultCurrency: 41500,
                            reportedAmountInDefaultCurrency: 37727,
                            attachments: [${testData.slurmReceipt.id}],
                            notes: "coffee",
                            percentOnBusiness: 100,
                            id: $expenseId,
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            tax: ${testData.slurmTax.id},
                            taxRateInBps: 1000,
                            taxAmount: 3773
                    }"""
                    )
                )
            }

        val expense = expenseRepository.findById(expenseId)
        assertThat(expense).isPresent.hasValueSatisfying {
            assertThat(it.category).isEqualTo(testData.slurmCategory)
        }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 if workspace belongs to another user when creating expense`(testData: ExpensesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(testData.defaultNewExpense())
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new expense with minimum data for default currency`(testData: ExpensesApiTestData) {
        val expenseId = dbHelper.getNextId()
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "ever best drink",
                            currency: "USD",
                            originalAmount: 150,
                            amountInDefaultCurrency: 150,
                            actualAmountInDefaultCurrency: 150,
                            reportedAmountInDefaultCurrency: 150,
                            percentOnBusiness: 100,
                            id: $expenseId,
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            attachments: [],
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when category of new expense is not found`(testData: ExpensesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": 537453,
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category 537453 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when category of new expense belongs to another workspace`(testData: ExpensesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.coffeeCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category ${testData.coffeeCategory.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when tax of new expense is not found`(testData: ExpensesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "tax": 4455
                }"""
            )
            .verifyNotFound("Tax 4455 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when tax of new expense belongs to another workspace`(testData: ExpensesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "tax": ${testData.coffeeTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.coffeeTax.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories/${testData.firstSlurm.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update expense of current user`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.beerCategory.id},
                    "title": "slurm -> beer",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "amountInDefaultCurrency": 30000,
                    "actualAmountInDefaultCurrency": 40000,
                    "attachments": [],
                    "notes": "beer",
                    "percentOnBusiness": 90,
                    "datePaid": "3000-02-02",
                    "tax": ${testData.slurmTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${testData.beerCategory.id},
                            title: "slurm -> beer",
                            currency: "HHD",
                            originalAmount: 20000,
                            amountInDefaultCurrency: 30000,
                            actualAmountInDefaultCurrency: 40000,
                            reportedAmountInDefaultCurrency: 32727,
                            attachments: [],
                            notes: "beer",
                            percentOnBusiness: 90,
                            id: ${testData.firstSlurm.id},
                            version: 1,
                            datePaid: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            tax: ${testData.slurmTax.id},
                            taxRateInBps: 1000,
                            taxAmount: 3273
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update expense of current user with minimum data`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "slurm updated",
                            currency: "HHD",
                            originalAmount: 20000,
                            attachments: [],
                            id: ${testData.firstSlurm.id},
                            version: 1,
                            datePaid: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PENDING_CONVERSION",
                            amountInDefaultCurrency: 0,
                            actualAmountInDefaultCurrency: 0,
                            reportedAmountInDefaultCurrency: 0,
                            percentOnBusiness: 100
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should fail with 404 on PUT when workspace belongs to another user`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when expense belongs to another workspace`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.coffeeExpense.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "title": "coffee updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Expense ${testData.coffeeExpense.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when expense does not exist`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/5566")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "title": "coffee updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Expense 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when category is not found`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": 5566,
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when category belongs to another workspace`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${testData.coffeeCategory.id},
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category ${testData.coffeeCategory.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when tax is not found`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "tax": 5566,
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when tax belongs to another workspace`(testData: ExpensesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "tax": ${testData.coffeeTax.id},
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax ${testData.coffeeTax.id} is not found")
    }

    class ExpensesApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val fryCoffeeWorkspace = Prototypes.workspace(owner = fry)
        val coffeeCategory = Prototypes.category(workspace = fryCoffeeWorkspace)
        val slurmCategory = Prototypes.category(workspace = fryWorkspace)
        val beerCategory = Prototypes.category(workspace = fryWorkspace)
        val coffeeTax = Prototypes.tax(workspace = fryCoffeeWorkspace)
        val slurmTax = Prototypes.tax(workspace = fryWorkspace)
        val slurmReceipt = Prototypes.document(workspace = fryWorkspace)

        val coffeeExpense = Prototypes.expense(
            workspace = coffeeCategory.workspace,
            category = coffeeCategory,
            currency = "THF",
            originalAmount = 50,
            amountInDefaultCurrency = 50,
            actualAmountInDefaultCurrency = 50,
            reportedAmountInDefaultCurrency = 50,
            percentOnBusiness = 100
        )

        val firstSlurm = Prototypes.expense(
            title = "best ever slurm",
            workspace = slurmCategory.workspace,
            category = slurmCategory,
            currency = "THF",
            originalAmount = 5000,
            amountInDefaultCurrency = 500,
            actualAmountInDefaultCurrency = 450,
            reportedAmountInDefaultCurrency = 450
        )

        val secondSlurm = Prototypes.expense(
            title = "another great slurm",
            workspace = slurmCategory.workspace,
            category = slurmCategory,
            currency = "ZZB",
            originalAmount = 5100,
            amountInDefaultCurrency = 510,
            actualAmountInDefaultCurrency = 460,
            reportedAmountInDefaultCurrency = 455,
            percentOnBusiness = 99,
            notes = "nice!",
            attachments = setOf(slurmReceipt),
            tax = null
        )

        override fun generateData() = listOf(
            farnsworth, fry, fryWorkspace, slurmCategory, slurmReceipt, firstSlurm, secondSlurm,
            fryCoffeeWorkspace, coffeeCategory, coffeeExpense,
            coffeeTax, slurmTax, beerCategory
        )

        fun defaultNewExpense(): String = """{
                    "category": ${slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [${slurmReceipt.id}],
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
    }
}