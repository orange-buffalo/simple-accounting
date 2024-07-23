package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME_VALUE
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentDate
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.infra.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@SimpleAccountingIntegrationTest
@DisplayName("Invoices API ")
internal class InvoicesApiControllerTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val timeService: TimeService,
    preconditionsFactory: PreconditionsFactory,
) {

    @BeforeEach
    fun setup() {
        mockCurrentTime(timeService)
    }

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return invoices of a workspace of current user`() {
        mockCurrentDate(timeService)

        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            customer: ${preconditions.spaceCustomer.id},
                            title: "first space invoice",
                            currency: "THF",
                            amount: 60,
                            attachments: [${preconditions.spaceDeliveryInvoicePrint.id}],
                            id: ${preconditions.firstSpaceInvoice.id},
                            version: 0,
                            dateIssued: "3000-01-01",
                            datePaid: "3000-01-04",
                            dateSent: "3000-01-03",
                            dueDate: "3000-01-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "DRAFT",
                            notes: "space notes",
                            generalTax: ${preconditions.planetExpressTax.id}
                    }"""
                    ),

                    json(
                        """{
                            customer: ${preconditions.anotherSpaceCustomer.id},
                            title: "second space invoice",
                            currency: "ZXF",
                            amount: 70,
                            attachments: [],
                            id: ${preconditions.secondSpaceInvoice.id},
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
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        client.get()
            .uri("/api/workspaces/27347947239/invoices")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an invoice only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/expenses/${preconditions.secondSpaceInvoice.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return invoice by id for current user`() {
        mockCurrentDate(timeService)

        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.secondSpaceInvoice.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            customer: ${preconditions.anotherSpaceCustomer.id},
                            title: "second space invoice",
                            currency: "ZXF",
                            amount: 70,
                            attachments: [],
                            id: ${preconditions.secondSpaceInvoice.id},
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
        client.get()
            .uri("/api/workspaces/5634632/invoices/${preconditions.firstSpaceInvoice.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting invoice by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if invoice belongs to another workspace when requesting invoice by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.pizzaInvoice.id}")
            .verifyNotFound("Invoice ${preconditions.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating invoice`() {
        client.post()
            .uri("/api/workspaces/995943/invoices")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new invoice`() {
        mockCurrentDate(timeService)

        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "customer": ${preconditions.spaceCustomer.id},
                    "title": "new invoice",
                    "currency": "TGF",
                    "amount": 400,
                    "attachments": [${preconditions.spaceDeliveryInvoicePrint.id}],
                    "dateIssued": "3000-02-01",
                    "datePaid": "3000-02-04",
                    "dateSent": "3000-02-03",
                    "dueDate": "3000-02-05",
                    "notes": "new space notes",
                    "generalTax": ${preconditions.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${preconditions.spaceCustomer.id},
                            title: "new invoice",
                            currency: "TGF",
                            amount: 400,
                            attachments: [${preconditions.spaceDeliveryInvoicePrint.id}],
                            id: "#{json-unit.any-number}",
                            version: 0,
                            dateIssued: "3000-02-01",
                            datePaid: "3000-02-04",
                            dateSent: "3000-02-03",
                            dueDate: "3000-02-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PAID",
                            notes: "new space notes",
                            generalTax: ${preconditions.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating invoice`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new invoice with minimum data`() {
        mockCurrentDate(timeService)

        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${preconditions.spaceCustomer.id},
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
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
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
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.pizzaCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyNotFound("Customer ${preconditions.pizzaCustomer.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new invoice is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
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
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "generalTax": ${preconditions.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${preconditions.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new invoice is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
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
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "attachments": [${preconditions.pizzaDeliveryInvoicePrint.id}]
                }"""
            )
            .verifyNotFound("Documents [${preconditions.pizzaDeliveryInvoicePrint.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update invoice of current user`() {
        mockCurrentDate(timeService)

        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.secondSpaceInvoice.id}")
            .sendJson(
                """{
                    "customer": ${preconditions.spaceCustomer.id},
                    "title": "updated invoice",
                    "currency": "TGF",
                    "amount": 400,
                    "attachments": [${preconditions.spaceDeliveryInvoicePrint.id}],
                    "dateIssued": "3000-02-01",
                    "datePaid": "3000-02-04",
                    "dateSent": "3000-02-03",
                    "dueDate": "3000-02-05",
                    "notes": "new space notes",
                    "generalTax": ${preconditions.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${preconditions.spaceCustomer.id},
                            title: "updated invoice",
                            currency: "TGF",
                            amount: 400,
                            attachments: [${preconditions.spaceDeliveryInvoicePrint.id}],
                            id: ${preconditions.secondSpaceInvoice.id},
                            version: 1,
                            dateIssued: "3000-02-01",
                            datePaid: "3000-02-04",
                            dateSent: "3000-02-03",
                            dueDate: "3000-02-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PAID",
                            notes: "new space notes",
                            generalTax: ${preconditions.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should update invoice of current user with minimum data`() {
        mockCurrentDate(timeService)

        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "updated invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${preconditions.spaceCustomer.id},
                            title: "updated invoice",
                            currency: "USD",
                            amount: 30000,
                            attachments: [],
                            id: ${preconditions.firstSpaceInvoice.id},
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
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when invoice belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.pizzaInvoice.id}")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Invoice ${preconditions.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when invoice does not exist`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/5566")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Invoice 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when customer is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
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
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "updated invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.pizzaCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02"
                }"""
            )
            .verifyNotFound("Customer ${preconditions.pizzaCustomer.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
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
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "generalTax": ${preconditions.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${preconditions.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
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
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}")
            .sendJson(
                """{
                    "title": "new invoice",
                    "currency": "USD",
                    "amount": 30000,
                    "customer": ${preconditions.spaceCustomer.id},
                    "dateIssued": "3000-02-01",
                    "dueDate": "3000-02-02",
                    "attachments": [${preconditions.pizzaDeliveryInvoicePrint.id}]
                }"""
            )
            .verifyNotFound("Documents [${preconditions.pizzaDeliveryInvoicePrint.id}] are not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on invoice cancellation when workspace belongs to another user`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}/cancel")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on invoice cancellation when invoice belongs to another workspace`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.pizzaInvoice.id}/cancel")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Invoice ${preconditions.pizzaInvoice.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on invoice cancellation when invoice does not exist`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/5566/cancel")
            .sendJson(preconditions.simpleInvoice())
            .verifyNotFound("Invoice 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should cancel invoice of current user`() {
        mockCurrentDate(timeService)

        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/invoices/${preconditions.firstSpaceInvoice.id}/cancel")
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            customer: ${preconditions.spaceCustomer.id},
                            title: "first space invoice",
                            currency: "THF",
                            amount: 60,
                            attachments: [${preconditions.spaceDeliveryInvoicePrint.id}],
                            id: ${preconditions.firstSpaceInvoice.id},
                            version: 1,
                            dateIssued: "3000-01-01",
                            datePaid: "3000-01-04",
                            dateSent: "3000-01-03",
                            dueDate: "3000-01-05",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "CANCELLED",
                            notes: "space notes",
                            generalTax: ${preconditions.planetExpressTax.id}
                        }"""
                    )
                )
            }
    }

    private val preconditions by preconditionsFactory {
        object {
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
}
