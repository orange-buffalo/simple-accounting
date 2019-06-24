package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepository
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Incomes API ")
internal class IncomesApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val dbHelper: DbHelper,
    @Autowired val incomeRepository: IncomeRepository
) {

    @MockBean
    lateinit var timeService: TimeService

    @Test
    fun `should allow GET access only for logged in users`(testData: IncomesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return incomes of a workspace of current user`(testData: IncomesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
                            title: "first space delivery",
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            id: ${testData.firstSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    ),

                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
                            title: "second space delivery",
                            currency: "ZZB",
                            originalAmount: 5100,
                            amountInDefaultCurrency: 510,
                            reportedAmountInDefaultCurrency: 455,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            notes: "nice!",
                            id: ${testData.secondSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found on GET`(testData: IncomesApiTestData) {
        client.get()
            .uri("/api/workspaces/27347947239/incomes")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 on GET if workspace belongs to another user`(testData: IncomesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return income by id for current user`(testData: IncomesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
                            title: "first space delivery",
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            id: ${testData.firstSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when requesting income by id`(testData: IncomesApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/incomes/${testData.firstSpaceIncome.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 if workspace belongs to another user when requesting income by id`(
        testData: IncomesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if income belongs to another workspace when requesting income by id`(
        testData: IncomesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.pizzaWageIncome.id}")
            .verifyNotFound("Income ${testData.pizzaWageIncome.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when creating income`(testData: IncomesApiTestData) {
        client.post()
            .uri("/api/workspaces/995943/incomes")
            .sendJson(testData.defaultNewIncome())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new income`(testData: IncomesApiTestData) {
        val incomeId = dbHelper.getNextId()
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new space delivery",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "reportedAmountInDefaultCurrency": 37727,
                    "attachments": [${testData.spaceDeliveryPayslip.id}],
                    "notes": "delivery",
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "tax": ${testData.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
                            title: "new space delivery",
                            currency: "AUD",
                            originalAmount: 30000,
                            amountInDefaultCurrency: 42000,
                            reportedAmountInDefaultCurrency: 37727,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            notes: "delivery",
                            id: $incomeId,
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            tax: ${testData.planetExpressTax.id},
                            taxRateInBps: 1000,
                            taxAmount: 3773
                    }"""
                    )
                )
            }

        val income = incomeRepository.findById(incomeId)
        assertThat(income).isPresent.hasValueSatisfying {
            assertThat(it.category).isEqualTo(testData.spaceDeliveryCategory)
        }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 if workspace belongs to another user when creating income`(testData: IncomesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(testData.defaultNewIncome())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new income with minimum data for default currency`(testData: IncomesApiTestData) {
        val incomeId = dbHelper.getNextId()
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
                            title: "new income",
                            currency: "USD",
                            originalAmount: 150,
                            amountInDefaultCurrency: 150,
                            reportedAmountInDefaultCurrency: 150,
                            id: $incomeId,
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
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
    fun `should return 404 when category of new income is not found`(testData: IncomesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": 537453,
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category 537453 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when category of new income belongs to another workspace`(testData: IncomesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.pizzaCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category ${testData.pizzaCategory.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when tax of new income is not found`(testData: IncomesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "tax": 4455
                }"""
            )
            .verifyNotFound("Tax 4455 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when tax of new income belongs to another workspace`(testData: IncomesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "tax": ${testData.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update income of current user`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": ${testData.pensionCategory.id},
                    "title": "space -> pension",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "amountInDefaultCurrency": 30000,
                    "reportedAmountInDefaultCurrency": 32727,
                    "attachments": [],
                    "notes": "pension",
                    "dateReceived": "3000-02-02",
                    "tax": ${testData.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${testData.pensionCategory.id},
                            title: "space -> pension",
                            currency: "HHD",
                            originalAmount: 20000,
                            amountInDefaultCurrency: 30000,
                            reportedAmountInDefaultCurrency: 32727,
                            attachments: [],
                            notes: "pension",
                            id: ${testData.firstSpaceIncome.id},
                            version: 1,
                            dateReceived: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            tax: ${testData.planetExpressTax.id},
                            taxRateInBps: 1000,
                            taxAmount: 3273
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update income of current user with minimum data`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "title": "delivery updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "delivery updated",
                            currency: "HHD",
                            originalAmount: 20000,
                            attachments: [],
                            id: ${testData.firstSpaceIncome.id},
                            version: 1,
                            dateReceived: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PENDING_CONVERSION",
                            amountInDefaultCurrency: 0,
                            reportedAmountInDefaultCurrency: 0
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should fail with 404 on PUT when workspace belongs to another user`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "title": "delivery updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when income belongs to another workspace`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.pizzaWageIncome.id}")
            .sendJson(
                """{
                    "title": "pizza updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Income ${testData.pizzaWageIncome.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when income does not exist`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/5566")
            .sendJson(
                """{
                    "title": "updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Income 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when category is not found`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": 5566,
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when category belongs to another workspace`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": ${testData.pizzaCategory.id},
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category ${testData.pizzaCategory.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when tax is not found`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "tax": 5566,
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when tax belongs to another workspace`(testData: IncomesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "tax": ${testData.pizzaDeliveryTax.id},
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    class IncomesApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val planetExpressWorkspace = Prototypes.workspace(owner = fry)
        val pizzaDeliveryWorkspace = Prototypes.workspace(owner = fry)
        val pizzaCategory = Prototypes.category(workspace = pizzaDeliveryWorkspace)
        val spaceDeliveryCategory = Prototypes.category(workspace = planetExpressWorkspace)
        val pensionCategory = Prototypes.category(workspace = planetExpressWorkspace)
        val pizzaDeliveryTax = Prototypes.tax(workspace = pizzaDeliveryWorkspace)
        val planetExpressTax = Prototypes.tax(workspace = planetExpressWorkspace)
        val spaceDeliveryPayslip = Prototypes.document(workspace = planetExpressWorkspace)

        val pizzaWageIncome = Prototypes.income(
            workspace = pizzaCategory.workspace,
            category = pizzaCategory,
            currency = "THF",
            originalAmount = 50,
            amountInDefaultCurrency = 50,
            reportedAmountInDefaultCurrency = 50
        )

        val firstSpaceIncome = Prototypes.income(
            title = "first space delivery",
            workspace = spaceDeliveryCategory.workspace,
            category = spaceDeliveryCategory,
            currency = "THF",
            originalAmount = 5000,
            amountInDefaultCurrency = 500,
            reportedAmountInDefaultCurrency = 450
        )

        val secondSpaceIncome = Prototypes.income(
            title = "second space delivery",
            workspace = spaceDeliveryCategory.workspace,
            category = spaceDeliveryCategory,
            currency = "ZZB",
            originalAmount = 5100,
            amountInDefaultCurrency = 510,
            reportedAmountInDefaultCurrency = 455,
            notes = "nice!",
            attachments = setOf(spaceDeliveryPayslip),
            tax = null
        )

        override fun generateData() = listOf(
            farnsworth, fry, planetExpressWorkspace, spaceDeliveryCategory, spaceDeliveryPayslip, 
            firstSpaceIncome, secondSpaceIncome,
            pizzaDeliveryWorkspace, pizzaCategory, pizzaWageIncome,
            pizzaDeliveryTax, planetExpressTax, pensionCategory
        )

        fun defaultNewIncome(): String = """{
                    "category": ${spaceDeliveryCategory.id},
                    "title": "new income",
                    "currency": "USD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "attachments": [${spaceDeliveryPayslip.id}],
                    "notes": "space delivery",
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
    }
}