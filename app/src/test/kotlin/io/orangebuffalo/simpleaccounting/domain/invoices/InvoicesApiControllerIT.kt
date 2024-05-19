package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME_VALUE
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentDate
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@SimpleAccountingIntegrationTest
@DisplayName("Invoices API ")
internal class InvoicesApiControllerIT(
    @Autowired private val client: WebTestClient,
    @Autowired private val timeService: TimeService,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @BeforeEach
    fun setup() {
        mockCurrentTime(timeService)
    }

    @Test
    fun `should allow GET access only for logged in users`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return invoices of a workspace of current user`() {
        val testData = setupPreconditions()
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
                            version: 0,
                            dateIssued: "3000-01-01",
                            datePaid: "3000-01-04",
                            dateSent: "3000-01-03",
                            dueDate: "3000-01-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "DRAFT",
                            notes: "space notes",
                            generalTax: ${testData.planetExpressTax.id}
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
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`() {
        setupPreconditions()
        client.get()
            .uri("/api/workspaces/27347947239/invoices")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an invoice only for logged in users`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/expenses/${testData.secondSpaceInvoice.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return invoice by id for current user`() {
        val testData = setupPreconditions()
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
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting invoice by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/5634632/invoices/${testData.firstSpaceInvoice.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting invoice by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if invoice belongs to another workspace when requesting invoice by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.pizzaInvoice.id}")
            .verifyNotFound("Invoice ${testData.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating invoice`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/995943/invoices")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new invoice`() {
        val testData = setupPreconditions()
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
                    "dateIssued": "3000-02-01",
                    "datePaid": "3000-02-04",
                    "dateSent": "3000-02-03",
                    "dueDate": "3000-02-05",
                    "notes": "new space notes",
                    "generalTax": ${testData.planetExpressTax.id}
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
                            version: 0,
                            dateIssued: "3000-02-01",
                            datePaid: "3000-02-04",
                            dateSent: "3000-02-03",
                            dueDate: "3000-02-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PAID",
                            notes: "new space notes",
                            generalTax: ${testData.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating invoice`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new invoice with minimum data`() {
        val testData = setupPreconditions()
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
    @WithMockFryUser
    fun `should return 404 when customer of new invoice is not found`() {
        val testData = setupPreconditions()
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
    @WithMockFryUser
    fun `should return 404 when customer of new invoice belongs to another workspace`() {
        val testData = setupPreconditions()
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
    @WithMockFryUser
    fun `should return 404 when tax of new invoice is not found`() {
        val testData = setupPreconditions()
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
                    "generalTax": 4455
                }"""
            )
            .verifyNotFound("Tax 4455 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new invoice belongs to another workspace`() {
        val testData = setupPreconditions()
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
                    "generalTax": ${testData.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new invoice is not found`() {
        val testData = setupPreconditions()
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
                    "attachments": [4455]
                }"""
            )
            .verifyNotFound("Documents [4455] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new invoice belongs to another workspace`() {
        val testData = setupPreconditions()
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
                    "attachments": [${testData.pizzaDeliveryInvoicePrint.id}]
                }"""
            )
            .verifyNotFound("Documents [${testData.pizzaDeliveryInvoicePrint.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update invoice of current user`() {
        val testData = setupPreconditions()
        mockCurrentDate(timeService)

        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.secondSpaceInvoice.id}")
            .sendJson(
                """{
                    "customer": ${testData.spaceCustomer.id},
                    "title": "updated invoice",
                    "currency": "TGF",
                    "amount": 400,
                    "attachments": [${testData.spaceDeliveryInvoicePrint.id}],
                    "dateIssued": "3000-02-01",
                    "datePaid": "3000-02-04",
                    "dateSent": "3000-02-03",
                    "dueDate": "3000-02-05",
                    "notes": "new space notes",
                    "generalTax": ${testData.planetExpressTax.id}
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
                            version: 1,
                            dateIssued: "3000-02-01",
                            datePaid: "3000-02-04",
                            dateSent: "3000-02-03",
                            dueDate: "3000-02-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PAID",
                            notes: "new space notes",
                            generalTax: ${testData.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should update invoice of current user with minimum data`() {
        val testData = setupPreconditions()
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
                            status: "DRAFT"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on PUT when workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when invoice belongs to another workspace`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.pizzaInvoice.id}")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Invoice ${testData.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when invoice does not exist`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/5566")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Invoice 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when customer is not found`() {
        val testData = setupPreconditions()
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
    @WithMockFryUser
    fun `should fail with 404 on PUT when customer belongs to another workspace`() {
        val testData = setupPreconditions()
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
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax is not found`() {
        val testData = setupPreconditions()
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
                    "generalTax": 5566
                }"""
            )
            .verifyNotFound("Tax 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax belongs to another workspace`() {
        val testData = setupPreconditions()
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
                    "generalTax": ${testData.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        val testData = setupPreconditions()
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
                    "attachments": [5566]
                }"""
            )
            .verifyNotFound("Documents [5566] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment belongs to another workspace`() {
        val testData = setupPreconditions()
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
                    "attachments": [${testData.pizzaDeliveryInvoicePrint.id}]
                }"""
            )
            .verifyNotFound("Documents [${testData.pizzaDeliveryInvoicePrint.id}] are not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on invoice cancellation when workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}/cancel")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on invoice cancellation when invoice belongs to another workspace`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.pizzaInvoice.id}/cancel")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Invoice ${testData.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on invoice cancellation when invoice does not exist`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/5566/cancel")
            .sendJson(testData.simpleInvoice())
            .verifyNotFound("Invoice 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should cancel invoice of current user`() {
        val testData = setupPreconditions()
        mockCurrentDate(timeService)

        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/invoices/${testData.firstSpaceInvoice.id}/cancel")
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${testData.spaceCustomer.id},
                            title: "first space invoice",
                            currency: "THF",
                            amount: 60,
                            attachments: [${testData.spaceDeliveryInvoicePrint.id}],
                            id: ${testData.firstSpaceInvoice.id},
                            version: 1,
                            dateIssued: "3000-01-01",
                            datePaid: "3000-01-04",
                            dateSent: "3000-01-03",
                            dueDate: "3000-01-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "CANCELLED",
                            notes: "space notes",
                            generalTax: ${testData.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()
        val farnsworth = farnsworth()
        val planetExpressWorkspace = workspace(owner = fry)
        val spaceCustomer = customer(workspace = planetExpressWorkspace)
        val anotherSpaceCustomer = customer(workspace = planetExpressWorkspace)
        val pizzaDeliveryWorkspace = workspace(owner = fry)
        val pizzaCustomer = customer(workspace = pizzaDeliveryWorkspace)
        val pizzaCategory = category(workspace = pizzaDeliveryWorkspace)
        val spaceDeliveryCategory = category(workspace = planetExpressWorkspace)
        val pensionCategory = category(workspace = planetExpressWorkspace)
        val pizzaDeliveryTax = generalTax(workspace = pizzaDeliveryWorkspace)
        val planetExpressTax = generalTax(workspace = planetExpressWorkspace)
        val spaceDeliveryInvoicePrint = document(workspace = planetExpressWorkspace)
        val pizzaDeliveryInvoicePrint = document(workspace = pizzaDeliveryWorkspace)

        val pizzaInvoice = invoice(
            title = "pizza invoice",
            customer = pizzaCustomer,
            currency = "THF",
            amount = 50,
            dateIssued = LocalDate.of(1999, 12, 20),
            dateSent = LocalDate.of(1999, 12, 22),
            datePaid = LocalDate.of(1999, 12, 23),
            dueDate = LocalDate.of(1999, 12, 24)
        )

        val firstSpaceInvoice = invoice(
            title = "first space invoice",
            customer = spaceCustomer,
            currency = "THF",
            amount = 60,
            dateIssued = LocalDate.of(3000, 1, 1),
            dateSent = LocalDate.of(3000, 1, 3),
            datePaid = LocalDate.of(3000, 1, 4),
            dueDate = LocalDate.of(3000, 1, 5),
            notes = "space notes",
            attachments = setOf(spaceDeliveryInvoicePrint),
            generalTax = planetExpressTax
        )

        val secondSpaceInvoice = invoice(
            title = "second space invoice",
            customer = anotherSpaceCustomer,
            currency = "ZXF",
            amount = 70,
            dateIssued = LocalDate.of(3000, 1, 6),
            dueDate = LocalDate.of(3000, 1, 7)
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
