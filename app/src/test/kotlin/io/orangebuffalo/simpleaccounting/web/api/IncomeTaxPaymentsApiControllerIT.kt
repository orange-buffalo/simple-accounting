package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE_VALUE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME_VALUE
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Income Tax Payments API ")
internal class IncomeTaxPaymentsApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val timeService: TimeService,
) {

    @BeforeEach
    fun setup() {
        mockCurrentTime(timeService)
    }

    @Test
    fun `should allow GET access only for logged in users`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return income tax payments of a workspace of current user`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            title: "first space income tax payment",
                            amount: 50,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            id: ${testData.firstSpaceIncomeTaxPayment.id},
                            notes: "tax? hah?",
                            version: 0,
                            reportingDate: "1999-04-07",
                            datePaid: "1999-03-30",
                            timeRecorded: "$MOCK_TIME_VALUE"
                    }"""
                    ),

                    json(
                        """{
                            title: "second space income tax payment",
                            amount: 100,
                            datePaid: "$MOCK_DATE_VALUE",
                            reportingDate: "$MOCK_DATE_VALUE",
                            id: ${testData.secondSpaceIncome.id},
                            version: 0,
                            timeRecorded: "$MOCK_TIME_VALUE",
                            attachments: []
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/27347947239/income-tax-payments")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an income only for logged in users`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/expenses/${testData.firstSpaceIncomeTaxPayment.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return income tax payment by id for current user`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            title: "first space income tax payment",
                            amount: 50,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            id: ${testData.firstSpaceIncomeTaxPayment.id},
                            notes: "tax? hah?",
                            version: 0,
                            reportingDate: "1999-04-07",
                            datePaid: "1999-03-30",
                            timeRecorded: "$MOCK_TIME_VALUE"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting income tax payment by id`(testData: IncomeTaxPaymentsApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting income tax payment by id`(
        testData: IncomeTaxPaymentsApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if income tax payment belongs to another workspace when requesting income tax payment by id`(
        testData: IncomeTaxPaymentsApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.pizzaIncomeTaxPayment.id}")
            .verifyNotFound("Income Tax Payment ${testData.pizzaIncomeTaxPayment.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating income tax payment`(testData: IncomeTaxPaymentsApiTestData) {
        client.post()
            .uri("/api/workspaces/995943/income-tax-payments")
            .sendJson(testData.defaultNewIncomeTaxPayment())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income tax payment`(testData: IncomeTaxPaymentsApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "attachments": [${testData.spaceDeliveryPayslip.id}],
                    "notes": "space delivery new tax payment",
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "new income tax payment",
                            amount: 30000,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            notes: "space delivery new tax payment",
                            datePaid: "$MOCK_DATE_VALUE",
                            reportingDate: "$MOCK_DATE_VALUE",
                            id: "#{json-unit.any-number}",
                            version: 0,
                            timeRecorded: "$MOCK_TIME_VALUE"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating income tax payment`(testData: IncomeTaxPaymentsApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(testData.defaultNewIncomeTaxPayment())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income with minimum data for default currency`(testData: IncomeTaxPaymentsApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "new income tax payment",
                            amount: 30000,
                            attachments: [],
                            datePaid: "$MOCK_DATE_VALUE",
                            reportingDate: "$MOCK_DATE_VALUE",
                            id: "#{json-unit.any-number}",
                            version: 0,
                            timeRecorded: "$MOCK_TIME_VALUE"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new income tax payment is not found`(testData: IncomeTaxPaymentsApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
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
    fun `should return 404 when attachment of new income tax payment belongs to another workspace`(testData: IncomeTaxPaymentsApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments")
            .sendJson(
                """{
                    "title": "new income tax payment",
                    "amount": 30000,
                    "attachments": [${testData.pizzaDeliveryPayslip.id}],
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Documents [${testData.pizzaDeliveryPayslip.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update income tax payment of current user`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .sendJson(
                """{
                    "title": "updated income tax payment",
                    "amount": 42,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "updated income tax payment",
                            amount: 42,
                            attachments: [],
                            datePaid: "$MOCK_DATE_VALUE",
                            reportingDate: "$MOCK_DATE_VALUE",
                            id: ${testData.firstSpaceIncomeTaxPayment.id},
                            version: 1,
                            timeRecorded: "$MOCK_TIME_VALUE"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on PUT when workspace belongs to another user`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .sendJson(testData.defaultNewIncomeTaxPayment())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income tax payment belongs to another workspace`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.pizzaIncomeTaxPayment.id}")
            .sendJson(testData.defaultNewIncomeTaxPayment())
            .verifyNotFound("Income Tax Payment ${testData.pizzaIncomeTaxPayment.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income tax payment does not exist`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/5566")
            .sendJson(testData.defaultNewIncomeTaxPayment())
            .verifyNotFound("Income Tax Payment 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
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
    fun `should fail with 404 on PUT when attachment belongs to another workspace`(testData: IncomeTaxPaymentsApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/income-tax-payments/${testData.firstSpaceIncomeTaxPayment.id}")
            .sendJson(
                """{
                    "title": "updated income tax payment",
                    "amount": 42,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "reportingDate": "$MOCK_DATE_VALUE",
                    "attachments": [${testData.pizzaDeliveryPayslip.id}]
                }"""
            )
            .verifyNotFound("Documents [${testData.pizzaDeliveryPayslip.id}] are not found")
    }

    class IncomeTaxPaymentsApiTestData : TestDataDeprecated {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val planetExpressWorkspace = Prototypes.workspace(owner = fry)
        val pizzaDeliveryWorkspace = Prototypes.workspace(owner = fry)
        val spaceDeliveryPayslip = Prototypes.document(workspace = planetExpressWorkspace)
        val pizzaDeliveryPayslip = Prototypes.document(workspace = pizzaDeliveryWorkspace)

        val pizzaIncomeTaxPayment = Prototypes.incomeTaxPayment(
            workspace = pizzaDeliveryWorkspace,
            amount = 50,
            title = "pizza income tax payment",
            attachments = setOf(pizzaDeliveryPayslip)
        )

        val firstSpaceIncomeTaxPayment = Prototypes.incomeTaxPayment(
            workspace = planetExpressWorkspace,
            amount = 50,
            title = "first space income tax payment",
            attachments = setOf(spaceDeliveryPayslip),
            notes = "tax? hah?",
            reportingDate = MOCK_DATE.plusDays(10),
            datePaid = MOCK_DATE.plusDays(2)
        )

        val secondSpaceIncome = Prototypes.incomeTaxPayment(
            workspace = planetExpressWorkspace,
            amount = 100,
            title = "second space income tax payment"
        )

        override fun generateData() = listOf(
            farnsworth, fry, planetExpressWorkspace, spaceDeliveryPayslip,
            firstSpaceIncomeTaxPayment, secondSpaceIncome,
            pizzaDeliveryWorkspace, pizzaDeliveryPayslip, pizzaIncomeTaxPayment
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
