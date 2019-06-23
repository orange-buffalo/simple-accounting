package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
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
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
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
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
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
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
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
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when creating expense`(testData: ExpensesApiTestData) {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/workspaces/995943/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(testData.defaultNewExpense())
            .verifyNotFound("Workspace 995943 is not found")
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
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
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

    class ExpensesApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()

        val fryWorkspace = Workspace(
            name = "Property of Philip J. Fry",
            owner = fry,
            taxEnabled = false,
            multiCurrencyEnabled = false,
            defaultCurrency = "USD"
        )

        val fryCoffeeWorkspace = Workspace(
            name = "Coffee Shop",
            owner = fry,
            taxEnabled = false,
            multiCurrencyEnabled = false,
            defaultCurrency = "USD"
        )

        val coffeeCategory = Category(
            name = "Coffee", workspace = fryCoffeeWorkspace, description = "..", income = false, expense = true
        )

        val slurmCategory = Category(
            name = "Slurm", workspace = fryWorkspace, description = "..", income = false, expense = true
        )

        val coffeeExpense = Expense(
            workspace = coffeeCategory.workspace,
            category = coffeeCategory,
            title = "100 cups",
            datePaid = MOCK_DATE,
            timeRecorded = MOCK_TIME,
            currency = "THF",
            originalAmount = 50,
            amountInDefaultCurrency = 50,
            actualAmountInDefaultCurrency = 50,
            reportedAmountInDefaultCurrency = 50,
            percentOnBusiness = 100,
            tax = null
        )

        val slurmReceipt = Document(
            name = "slurm",
            workspace = fryWorkspace,
            storageProviderId = "local-fs",
            storageProviderLocation = "lost",
            timeUploaded = MOCK_TIME,
            sizeInBytes = 30
        )

        val firstSlurm = Expense(
            workspace = slurmCategory.workspace,
            category = slurmCategory,
            title = "best ever slurm",
            datePaid = MOCK_DATE,
            timeRecorded = MOCK_TIME,
            currency = "THF",
            originalAmount = 5000,
            amountInDefaultCurrency = 500,
            actualAmountInDefaultCurrency = 450,
            reportedAmountInDefaultCurrency = 450,
            percentOnBusiness = 100,
            tax = null
        )

        val secondSlurm = Expense(
            workspace = slurmCategory.workspace,
            category = slurmCategory,
            title = "another great slurm",
            datePaid = MOCK_DATE,
            timeRecorded = MOCK_TIME,
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

        val coffeeTax = Tax(
            title = "cofee",
            rateInBps = 10,
            workspace = fryCoffeeWorkspace
        )

        val slurmTax = Tax(
            title = "slurm",
            rateInBps = 10_00,
            workspace = fryWorkspace
        )

        override fun generateData() = listOf(
            farnsworth, fry, fryWorkspace, slurmCategory, slurmReceipt, firstSlurm, secondSlurm,
            fryCoffeeWorkspace, coffeeCategory, coffeeExpense,
            coffeeTax, slurmTax
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