package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE_VALUE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@DisplayName("Income Tax Payments API ")
internal class IncomeTaxPaymentsApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return income tax payments of a workspace of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("title", "first space income tax payment")
                        put("amount", 50)
                        putJsonArray("attachments") {
                            add(preconditions.spaceDeliveryPayslip.id)
                        }
                        put("id", preconditions.firstSpaceIncomeTaxPayment.id)
                        put("notes", "tax? hah?")
                        put("reportingDate", "1999-04-07")
                        put("datePaid", "1999-03-30")
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("version", 0)
                    }
                    addJsonObject {
                        put("title", "second space income tax payment")
                        put("amount", 100)
                        put("datePaid", MOCK_DATE_VALUE)
                        put("reportingDate", MOCK_DATE_VALUE)
                        put("id", preconditions.secondSpaceIncome.id)
                        put("version", 0)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        putJsonArray("attachments") {}
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        client.get()
            .uri("/api/workspaces/27347947239/income-tax-payments")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return income tax payments with pagination parameters`() {
        client.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/workspaces/{workspaceId}/income-tax-payments")
                    .queryParam("pageNumber", "1")
                    .queryParam("pageSize", "1")
                    .build(preconditions.planetExpressWorkspace.id)
            }
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 1)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("title", "first space income tax payment")
                        put("amount", 50)
                        putJsonArray("attachments") {
                            add(preconditions.spaceDeliveryPayslip.id)
                        }
                        put("id", preconditions.firstSpaceIncomeTaxPayment.id)
                        put("notes", "tax? hah?")
                        put("reportingDate", "1999-04-07")
                        put("datePaid", "1999-03-30")
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("version", 0)
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should return second page of income tax payments`() {
        client.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/workspaces/{workspaceId}/income-tax-payments")
                    .queryParam("pageNumber", "2")
                    .queryParam("pageSize", "1")
                    .build(preconditions.planetExpressWorkspace.id)
            }
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 2)
                put("pageSize", 1)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("title", "second space income tax payment")
                        put("amount", 100)
                        put("datePaid", MOCK_DATE_VALUE)
                        put("reportingDate", MOCK_DATE_VALUE)
                        put("id", preconditions.secondSpaceIncome.id)
                        put("version", 0)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        putJsonArray("attachments") {}
                    }
                }
            }
    }

    @Test
    fun `should allow GET access for an income only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/expenses/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return income tax payment by id for current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .verifyOkAndJsonBody(
                """{
                    title: "first space income tax payment",
                    amount: 50,
                    attachments: [${preconditions.spaceDeliveryPayslip.id}],
                    id: ${preconditions.firstSpaceIncomeTaxPayment.id},
                    notes: "tax? hah?",
                    version: 0,
                    reportingDate: "1999-04-07",
                    datePaid: "1999-03-30",
                    timeRecorded: "$MOCK_TIME_VALUE"
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting income tax payment by id`() {
        client.get()
            .uri("/api/workspaces/5634632/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting income tax payment by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if income tax payment belongs to another workspace when requesting income tax payment by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.pizzaIncomeTaxPayment.id}")
            .verifyNotFound("Income Tax Payment ${preconditions.pizzaIncomeTaxPayment.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating income tax payment`() {
        client.post()
            .uri("/api/workspaces/995943/income-tax-payments")
            .sendJson(preconditions.defaultNewIncomeTaxPayment())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income tax payment`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "attachments": [${preconditions.spaceDeliveryPayslip.id}],
                    "notes": "space delivery new tax payment",
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "new income tax payment",
                    amount: 30000,
                    attachments: [${preconditions.spaceDeliveryPayslip.id}],
                    notes: "space delivery new tax payment",
                    datePaid: "$MOCK_DATE_VALUE",
                    reportingDate: "$MOCK_DATE_VALUE",
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    timeRecorded: "$MOCK_TIME_VALUE"
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating income tax payment`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(preconditions.defaultNewIncomeTaxPayment())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income with minimum data for default currency`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "new income tax payment",
                    amount: 30000,
                    attachments: [],
                    datePaid: "$MOCK_DATE_VALUE",
                    reportingDate: "$MOCK_DATE_VALUE",
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    timeRecorded: "$MOCK_TIME_VALUE"
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new income tax payment is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "attachments": [4455],
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Documents [4455] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new income tax payment belongs to another workspace`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "attachments": [${preconditions.pizzaDeliveryPayslip.id}],
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Documents [${preconditions.pizzaDeliveryPayslip.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update income tax payment of current user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .sendJson(
                """{
                    "title": "updated income tax payment",
                    "amount": 42,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "updated income tax payment",
                    amount: 42,
                    attachments: [],
                    datePaid: "$MOCK_DATE_VALUE",
                    reportingDate: "$MOCK_DATE_VALUE",
                    id: ${preconditions.firstSpaceIncomeTaxPayment.id},
                    version: 1,
                    timeRecorded: "$MOCK_TIME_VALUE"
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on PUT when workspace belongs to another user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .sendJson(preconditions.defaultNewIncomeTaxPayment())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income tax payment belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.pizzaIncomeTaxPayment.id}")
            .sendJson(preconditions.defaultNewIncomeTaxPayment())
            .verifyNotFound("Income Tax Payment ${preconditions.pizzaIncomeTaxPayment.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income tax payment does not exist`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/5566")
            .sendJson(preconditions.defaultNewIncomeTaxPayment())
            .verifyNotFound("Income Tax Payment 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .sendJson(
                """{
                    "title": "updated income tax payment",
                    "amount": 42,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE",
                    "attachments": [5566]
                }"""
            )
            .verifyNotFound("Documents [5566] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/income-tax-payments/${preconditions.firstSpaceIncomeTaxPayment.id}")
            .sendJson(
                """{
                    "title": "updated income tax payment",
                    "amount": 42,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE",
                    "attachments": [${preconditions.pizzaDeliveryPayslip.id}]
                }"""
            )
            .verifyNotFound("Documents [${preconditions.pizzaDeliveryPayslip.id}] are not found")
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val planetExpressWorkspace = workspace(owner = fry)
            val pizzaDeliveryWorkspace = workspace(owner = fry)
            val spaceDeliveryPayslip = document(workspace = planetExpressWorkspace)
            val pizzaDeliveryPayslip = document(workspace = pizzaDeliveryWorkspace)

            val pizzaIncomeTaxPayment = incomeTaxPayment(
                workspace = pizzaDeliveryWorkspace,
                amount = 50,
                title = "pizza income tax payment",
                attachments = setOf(pizzaDeliveryPayslip)
            )

            val firstSpaceIncomeTaxPayment = incomeTaxPayment(
                workspace = planetExpressWorkspace,
                amount = 50,
                title = "first space income tax payment",
                attachments = setOf(spaceDeliveryPayslip),
                notes = "tax? hah?",
                reportingDate = MOCK_DATE.plusDays(10),
                datePaid = MOCK_DATE.plusDays(2)
            )

            val secondSpaceIncome = incomeTaxPayment(
                workspace = planetExpressWorkspace,
                amount = 100,
                title = "second space income tax payment"
            )

            fun defaultNewIncomeTaxPayment(): String = """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "attachments": [${spaceDeliveryPayslip.id}],
                    "notes": "space delivery tax payment",
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
        }
    }
}
