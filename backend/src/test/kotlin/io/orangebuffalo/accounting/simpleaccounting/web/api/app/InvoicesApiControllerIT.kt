package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.*
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
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
import java.time.LocalDate

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Invoices API ")
internal class InvoicesApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @MockBean
    lateinit var timeService: TimeService

    @Test
    fun `should allow GET access only for logged in users`(testData: InvoicesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return invoices of a workspace of current user`(testData: InvoicesApiTestData) {
        mockCurrentDate(timeService)

        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            customer: ${testData.spaceCustomer.id},
                            title: "first space invoice",
                            currency: "THF",
                            amount: 60,
                            attachments: [${testData.spaceDeliveryInvoicePrint.id}],
                            id: ${testData.firstSpaceInvoice.id},
                            income: ${testData.spaceIncome.id},
                            version: 0,
                            dateCancelled: "3000-01-02",
                            dateIssued: "3000-01-01",
                            datePaid: "3000-01-04",
                            dateSent: "3000-01-03",
                            dueDate: "3000-01-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "CANCELLED",
                            notes: "space notes",
                            tax: ${testData.planetExpressTax.id}
                    }"""
                    ),

                    json(
                        """{
                            customer: ${testData.anotherSpaceCustomer.id},
                            title: "second space invoice",
                            currency: "ZXF",
                            amount: 70,
                            attachments: [],
                            id: ${testData.secondSpaceInvoice.id},
                            version: 0,
                            dateIssued: "3000-01-06",
                            dueDate: "3000-01-07",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "DRAFT"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found on GET`(testData: InvoicesApiTestData) {
        client.get()
            .uri("/api/workspaces/27347947239/invoices")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 on GET if workspace belongs to another user`(testData: InvoicesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an invoice only for logged in users`(testData: InvoicesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/expenses/${testData.secondSpaceInvoice.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return invoice by id for current user`(testData: InvoicesApiTestData) {
        mockCurrentDate(timeService)

        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.secondSpaceInvoice.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            customer: ${testData.anotherSpaceCustomer.id},
                            title: "second space invoice",
                            currency: "ZXF",
                            amount: 70,
                            attachments: [],
                            id: ${testData.secondSpaceInvoice.id},
                            version: 0,
                            dateIssued: "3000-01-06",
                            dueDate: "3000-01-07",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "DRAFT"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when requesting invoice by id`(testData: InvoicesApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/invoices/${testData.firstSpaceInvoice.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 if workspace belongs to another user when requesting invoice by id`(
        testData: InvoicesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if invoice belongs to another workspace when requesting invoice by id`(
        testData: InvoicesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.pizzaInvoice.id}")
            .verifyNotFound("Invoice ${testData.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when creating invoice`(testData: InvoicesApiTestData) {
        client.post()
            .uri("/api/workspaces/995943/invoices")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new invoice`(testData: InvoicesApiTestData) {
        mockCurrentTime(timeService)
        mockCurrentDate(timeService)

        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "customer": ${testData.spaceCustomer.id},
                    "title": "new invoice",
                    "currency": "TGF",
                    "amount": 400,
                    "attachments": [${testData.spaceDeliveryInvoicePrint.id}],
                    "dateCancelled": "3000-02-02",
                    "dateIssued": "3000-02-01",
                    "datePaid": "3000-02-04",
                    "dateSent": "3000-02-03",
                    "dueDate": "3000-02-05",
                    "notes": "new space notes",
                    "tax": ${testData.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${testData.spaceCustomer.id},
                            title: "new invoice",
                            currency: "TGF",
                            amount: 400,
                            attachments: [${testData.spaceDeliveryInvoicePrint.id}],
                            id: "#{json-unit.any-number}",
                            income: "#{json-unit.any-number}",
                            version: 0,
                            dateCancelled: "3000-02-02",
                            dateIssued: "3000-02-01",
                            datePaid: "3000-02-04",
                            dateSent: "3000-02-03",
                            dueDate: "3000-02-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "CANCELLED",
                            notes: "new space notes",
                            tax: ${testData.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return 404 if workspace belongs to another user when creating invoice`(testData: InvoicesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new invoice with minimum data`(testData: InvoicesApiTestData) {
        mockCurrentTime(timeService)
        mockCurrentDate(timeService)

        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${testData.spaceCustomer.id},
                            title: "new invoice",
                            currency: "USD",
                            amount: 30000,
                            attachments: [],
                            id: "#{json-unit.any-number}",
                            version: 0,
                            dateIssued: "3000-02-01",
                            dueDate: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "DRAFT"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when customer of new invoice is not found`(testData: InvoicesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": 537453,
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyNotFound("Customer 537453 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when customer of new invoice belongs to another workspace`(testData: InvoicesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.pizzaCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyNotFound("Customer ${testData.pizzaCustomer.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when tax of new invoice is not found`(testData: InvoicesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "tax": 4455
                }"""
            )
            .verifyNotFound("Tax 4455 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 when tax of new invoice belongs to another workspace`(testData: InvoicesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "tax": ${testData.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update invoice of current user`(testData: InvoicesApiTestData) {
        mockCurrentDate(timeService)
        mockCurrentTime(timeService)

        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.secondSpaceInvoice.id}")
            .sendJson(
                """{
                    "customer": ${testData.spaceCustomer.id},
                    "title": "updated invoice",
                    "currency": "TGF",
                    "amount": 400,
                    "attachments": [${testData.spaceDeliveryInvoicePrint.id}],
                    "dateCancelled": "3000-02-02",
                    "dateIssued": "3000-02-01",
                    "datePaid": "3000-02-04",
                    "dateSent": "3000-02-03",
                    "dueDate": "3000-02-05",
                    "notes": "new space notes",
                    "tax": ${testData.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${testData.spaceCustomer.id},
                            title: "updated invoice",
                            currency: "TGF",
                            amount: 400,
                            attachments: [${testData.spaceDeliveryInvoicePrint.id}],
                            id: ${testData.secondSpaceInvoice.id},
                            income: "#{json-unit.any-number}",
                            version: 1,
                            dateCancelled: "3000-02-02",
                            dateIssued: "3000-02-01",
                            datePaid: "3000-02-04",
                            dateSent: "3000-02-03",
                            dueDate: "3000-02-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "CANCELLED",
                            notes: "new space notes",
                            tax: ${testData.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update invoice of current user with minimum data`(testData: InvoicesApiTestData) {
        mockCurrentDate(timeService)

        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "updated invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${testData.spaceCustomer.id},
                            title: "updated invoice",
                            currency: "USD",
                            amount: 30000,
                            attachments: [],
                            id: ${testData.firstSpaceInvoice.id},
                            version: 1,
                            dateIssued: "3000-02-01",
                            dueDate: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "DRAFT",
                            income: ${testData.spaceIncome.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should fail with 404 on PUT when workspace belongs to another user`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when invoice belongs to another workspace`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.pizzaInvoice.id}")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Invoice ${testData.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when invoice does not exist`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/5566")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Invoice 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when customer is not found`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "update invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": 5566,
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyNotFound("Customer 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when customer belongs to another workspace`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "updated invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.pizzaCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyNotFound("Customer ${testData.pizzaCustomer.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when tax is not found`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "tax": 5566
                }"""
            )
            .verifyNotFound("Tax 5566 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should fail with 404 on PUT when tax belongs to another workspace`(testData: InvoicesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${testData.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "tax": ${testData.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    class InvoicesApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val planetExpressWorkspace = Prototypes.workspace(owner = fry)
        val spaceCustomer = Prototypes.customer(workspace = planetExpressWorkspace)
        val anotherSpaceCustomer = Prototypes.customer(workspace = planetExpressWorkspace)
        val pizzaDeliveryWorkspace = Prototypes.workspace(owner = fry)
        val pizzaCustomer = Prototypes.customer(workspace = pizzaDeliveryWorkspace)
        val pizzaCategory = Prototypes.category(workspace = pizzaDeliveryWorkspace)
        val spaceDeliveryCategory = Prototypes.category(workspace = planetExpressWorkspace)
        val pensionCategory = Prototypes.category(workspace = planetExpressWorkspace)
        val pizzaDeliveryTax = Prototypes.tax(workspace = pizzaDeliveryWorkspace)
        val planetExpressTax = Prototypes.tax(workspace = planetExpressWorkspace)
        val spaceDeliveryInvoicePrint = Prototypes.document(workspace = planetExpressWorkspace)

        val pizzaInvoice = Prototypes.invoice(
            title = "pizza invoice",
            customer = pizzaCustomer,
            currency = "THF",
            amount = 50,
            dateIssued = LocalDate.of(1999, 12, 20),
            dateCancelled = LocalDate.of(1999, 12, 21),
            dateSent = LocalDate.of(1999, 12, 22),
            datePaid = LocalDate.of(1999, 12, 23),
            dueDate = LocalDate.of(1999, 12, 24)
        )

        val spaceIncome = Prototypes.income(category = spaceDeliveryCategory)
        val firstSpaceInvoice = Prototypes.invoice(
            title = "first space invoice",
            customer = spaceCustomer,
            currency = "THF",
            amount = 60,
            dateIssued = LocalDate.of(3000, 1, 1),
            dateCancelled = LocalDate.of(3000, 1, 2),
            dateSent = LocalDate.of(3000, 1, 3),
            datePaid = LocalDate.of(3000, 1, 4),
            dueDate = LocalDate.of(3000, 1, 5),
            notes = "space notes",
            attachments = setOf(spaceDeliveryInvoicePrint),
            income = spaceIncome,
            tax = planetExpressTax
        )

        val secondSpaceInvoice = Prototypes.invoice(
            title = "second space invoice",
            customer = anotherSpaceCustomer,
            currency = "ZXF",
            amount = 70,
            dateIssued = LocalDate.of(3000, 1, 6),
            dueDate = LocalDate.of(3000, 1, 7)
        )

        override fun generateData() = listOf(
            farnsworth, fry, planetExpressWorkspace, pizzaDeliveryWorkspace,
            spaceDeliveryCategory, pensionCategory, pizzaCategory,
            spaceDeliveryInvoicePrint,
            pizzaDeliveryTax, planetExpressTax,
            spaceCustomer, anotherSpaceCustomer, pizzaCustomer,
            spaceIncome,
            firstSpaceInvoice, secondSpaceInvoice, pizzaInvoice
        )

        fun simpleInvoice(): String = """{
            "title": "new invoice",
            "currency": "USD",
            "amount": 30000,
            "customer": ${spaceCustomer.id},
            "attachments": [${spaceDeliveryInvoicePrint.id}],
            "notes": "space delivery",
            "dateIssued": "3000-02-01",
            "dueDate": "3000-02-02"
        }"""
    }
}