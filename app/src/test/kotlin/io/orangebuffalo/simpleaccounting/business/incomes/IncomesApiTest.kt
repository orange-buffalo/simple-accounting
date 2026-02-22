package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE_VALUE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import kotlinx.serialization.json.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@DisplayName("Incomes API")
internal class IncomesApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return incomes of a workspace of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 3)
                putJsonArray("data") {
                    addJsonObject {
                        put("category", preconditions.spaceDeliveryCategory.id)
                        put("title", "first space delivery")
                        put("currency", "THF")
                        put("originalAmount", 5000)
                        putJsonObject("convertedAmounts") {
                            put("originalAmountInDefaultCurrency", 520)
                            put("adjustedAmountInDefaultCurrency", 500)
                        }
                        putJsonObject("incomeTaxableAmounts") {
                            put("originalAmountInDefaultCurrency", 450)
                            put("adjustedAmountInDefaultCurrency", 430)
                        }
                        put("useDifferentExchangeRateForIncomeTaxPurposes", true)
                        putJsonArray("attachments") {}
                        put("id", preconditions.firstSpaceIncome.id)
                        put("version", 0)
                        put("dateReceived", MOCK_DATE_VALUE)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("status", "FINALIZED")
                        put("generalTax", preconditions.spaceTax.id)
                        put("generalTaxAmount", 20)
                        put("generalTaxRateInBps", 12000)
                        put("linkedInvoice", preconditions.firstSpaceInvoice.id)
                    }
                    addJsonObject {
                        put("category", preconditions.spaceDeliveryCategory.id)
                        put("title", "second space delivery")
                        put("currency", "ZZB")
                        put("originalAmount", 5100)
                        putJsonObject("convertedAmounts") {
                            put("originalAmountInDefaultCurrency", 510)
                            put("adjustedAmountInDefaultCurrency", 510)
                        }
                        putJsonObject("incomeTaxableAmounts") {}
                        put("useDifferentExchangeRateForIncomeTaxPurposes", true)
                        putJsonArray("attachments") {
                            add(preconditions.spaceDeliveryPayslip.id)
                        }
                        put("notes", "nice!")
                        put("id", preconditions.secondSpaceIncome.id)
                        put("version", 0)
                        put("dateReceived", MOCK_DATE_VALUE)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("status", "PENDING_CONVERSION_FOR_TAXATION_PURPOSES")
                    }
                    addJsonObject {
                        put("title", "third space delivery")
                        put("currency", "ZZA")
                        put("originalAmount", 200)
                        putJsonObject("convertedAmounts") {}
                        putJsonObject("incomeTaxableAmounts") {}
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        putJsonArray("attachments") {}
                        put("id", preconditions.thirdSpaceIncome.id)
                        put("version", 0)
                        put("dateReceived", MOCK_DATE_VALUE)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("status", "PENDING_CONVERSION")
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
            .uri("/api/workspaces/27347947239/incomes")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an income only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/expenses/${preconditions.firstSpaceIncome.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return income by id for current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.spaceDeliveryCategory.id},
                    title: "first space delivery",
                    currency: "THF",
                    originalAmount: 5000,
                    convertedAmounts: {
                        originalAmountInDefaultCurrency: 520,
                        adjustedAmountInDefaultCurrency: 500
                    },
                    incomeTaxableAmounts: {
                        originalAmountInDefaultCurrency: 450,
                        adjustedAmountInDefaultCurrency: 430
                    },
                    useDifferentExchangeRateForIncomeTaxPurposes: true,
                    attachments: [],
                    id: ${preconditions.firstSpaceIncome.id},
                    version: 0,
                    dateReceived: "$MOCK_DATE_VALUE",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "FINALIZED",
                    generalTax: ${preconditions.spaceTax.id},
                    generalTaxAmount: 20,
                    generalTaxRateInBps: 12000,
                    linkedInvoice: ${preconditions.firstSpaceInvoice.id}
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting income by id`() {
        client.get()
            .uri("/api/workspaces/5634632/incomes/${preconditions.firstSpaceIncome.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting income by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if income belongs to another workspace when requesting income by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.pizzaWageIncome.id}")
            .verifyNotFound("Income ${preconditions.pizzaWageIncome.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating income`() {
        client.post()
            .uri("/api/workspaces/995943/incomes")
            .sendJson(preconditions.defaultNewIncome())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${preconditions.spaceDeliveryCategory.id},
                    "title": "new space delivery",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "convertedAmountInDefaultCurrency": 42000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "incomeTaxableAmountInDefaultCurrency": 37727,
                    "attachments": [${preconditions.spaceDeliveryPayslip.id}],
                    "notes": "delivery",
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "generalTax": ${preconditions.planetExpressTax.id}
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.spaceDeliveryCategory.id},
                    title: "new space delivery",
                    currency: "AUD",
                    originalAmount: 30000,
                    convertedAmounts: {
                        originalAmountInDefaultCurrency: 42000,
                        adjustedAmountInDefaultCurrency: 38182
                    },
                    incomeTaxableAmounts: {
                        originalAmountInDefaultCurrency: 37727,
                        adjustedAmountInDefaultCurrency: 34297
                    },
                    useDifferentExchangeRateForIncomeTaxPurposes: true,
                    attachments: [${preconditions.spaceDeliveryPayslip.id}],
                    notes: "delivery",
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    dateReceived: "$MOCK_DATE_VALUE",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "FINALIZED",
                    generalTax: ${preconditions.planetExpressTax.id},
                    generalTaxRateInBps: 1000,
                    generalTaxAmount: 3430
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating income`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(preconditions.defaultNewIncome())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income with minimum data for default currency`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${preconditions.spaceDeliveryCategory.id},
                    "title": "new income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.spaceDeliveryCategory.id},
                    title: "new income",
                    currency: "USD",
                    originalAmount: 150,
                    convertedAmounts: {
                        originalAmountInDefaultCurrency: 150,
                        adjustedAmountInDefaultCurrency: 150
                    },
                    incomeTaxableAmounts: {
                        originalAmountInDefaultCurrency: 150,
                        adjustedAmountInDefaultCurrency: 150
                    },
                    useDifferentExchangeRateForIncomeTaxPurposes: false,
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    dateReceived: "$MOCK_DATE_VALUE",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    attachments: [],
                    status: "FINALIZED"
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when category of new income is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": 537453,
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category 537453 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when category of new income belongs to another workspace`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${preconditions.pizzaCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category ${preconditions.pizzaCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new income is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${preconditions.spaceDeliveryCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "generalTax": 4455
                }"""
            )
            .verifyNotFound("Tax 4455 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new income belongs to another workspace`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${preconditions.spaceDeliveryCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "generalTax": ${preconditions.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${preconditions.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new income is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "attachments": [4455]
                }"""
            )
            .verifyNotFound("Documents [4455] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new income belongs to another workspace`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "attachments": [${preconditions.pizzaDeliveryPayslip.id}]
                }"""
            )
            .verifyNotFound("Documents [${preconditions.pizzaDeliveryPayslip.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update income of current user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": ${preconditions.pensionCategory.id},
                    "title": "space -> pension",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "convertedAmountInDefaultCurrency": 30000,
                    "incomeTaxableAmountInDefaultCurrency": 32727,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "attachments": [],
                    "notes": "pension",
                    "dateReceived": "3000-02-02",
                    "generalTax": ${preconditions.planetExpressTax.id},
                    "linkedInvoice": ${preconditions.secondSpaceInvoice.id}
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.pensionCategory.id},
                    title: "space -> pension",
                    currency: "HHD",
                    originalAmount: 20000,
                    convertedAmounts: {
                        originalAmountInDefaultCurrency: 30000,
                        adjustedAmountInDefaultCurrency: 27273
                    },
                    incomeTaxableAmounts: {
                        originalAmountInDefaultCurrency: 32727,
                        adjustedAmountInDefaultCurrency: 29752
                    },
                    useDifferentExchangeRateForIncomeTaxPurposes: true,
                    attachments: [],
                    notes: "pension",
                    id: ${preconditions.firstSpaceIncome.id},
                    version: 1,
                    dateReceived: "3000-02-02",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "FINALIZED",
                    generalTax: ${preconditions.planetExpressTax.id},
                    generalTaxRateInBps: 1000,
                    generalTaxAmount: 2975,
                    linkedInvoice: ${preconditions.secondSpaceInvoice.id}
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should update income of current user with minimum data`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "title": "delivery updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "delivery updated",
                    currency: "HHD",
                    originalAmount: 20000,
                    attachments: [],
                    id: ${preconditions.firstSpaceIncome.id},
                    version: 1,
                    dateReceived: "3000-02-02",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "PENDING_CONVERSION",
                    convertedAmounts: {
                    },
                    incomeTaxableAmounts: {
                    },
                    useDifferentExchangeRateForIncomeTaxPurposes: false
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on PUT when workspace belongs to another user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "title": "delivery updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.pizzaWageIncome.id}")
            .sendJson(
                """{
                    "title": "pizza updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Income ${preconditions.pizzaWageIncome.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income does not exist`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/5566")
            .sendJson(
                """{
                    "title": "updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Income 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when category is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": 5566,
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when category belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": ${preconditions.pizzaCategory.id},
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category ${preconditions.pizzaCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "generalTax": 5566,
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "generalTax": ${preconditions.pizzaDeliveryTax.id},
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax ${preconditions.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "attachments": [5566],
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Documents [5566] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/incomes/${preconditions.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "attachments": [${preconditions.pizzaDeliveryPayslip.id}],
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
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
            val pizzaCategory = category(workspace = pizzaDeliveryWorkspace)
            val spaceDeliveryCategory = category(workspace = planetExpressWorkspace)
            val pensionCategory = category(workspace = planetExpressWorkspace)
            val pizzaDeliveryTax = generalTax(workspace = pizzaDeliveryWorkspace)
            val planetExpressTax = generalTax(workspace = planetExpressWorkspace, rateInBps = 1000)
            val spaceDeliveryPayslip = document(workspace = planetExpressWorkspace)
            val pizzaDeliveryPayslip = document(workspace = pizzaDeliveryWorkspace)
            val spaceTax = generalTax(workspace = planetExpressWorkspace, rateInBps = 1000)

            val pizzaWageIncome = income(
                workspace = pizzaDeliveryWorkspace,
                category = pizzaCategory,
                currency = "THF",
                originalAmount = 50,
                convertedAmounts = amountsInDefaultCurrency(50),
                incomeTaxableAmounts = amountsInDefaultCurrency(50)
            )

            val spaceCustomer = customer(
                workspace = planetExpressWorkspace
            )

            val firstSpaceInvoice = invoice(
                customer = spaceCustomer
            )

            val secondSpaceInvoice = invoice(
                customer = spaceCustomer
            )

            val firstSpaceIncome = income(
                title = "first space delivery",
                workspace = planetExpressWorkspace,
                category = spaceDeliveryCategory,
                currency = "THF",
                originalAmount = 5000,
                convertedAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = 520,
                    adjustedAmountInDefaultCurrency = 500
                ),
                incomeTaxableAmounts = AmountsInDefaultCurrency(
                    originalAmountInDefaultCurrency = 450,
                    adjustedAmountInDefaultCurrency = 430
                ),
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                status = IncomeStatus.FINALIZED,
                generalTax = spaceTax,
                generalTaxAmount = 20,
                generalTaxRateInBps = 12000,
                linkedInvoice = firstSpaceInvoice
            )

            val secondSpaceIncome = income(
                title = "second space delivery",
                workspace = planetExpressWorkspace,
                category = spaceDeliveryCategory,
                currency = "ZZB",
                originalAmount = 5100,
                convertedAmounts = amountsInDefaultCurrency(510),
                incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                useDifferentExchangeRateForIncomeTaxPurposes = true,
                notes = "nice!",
                attachments = setOf(spaceDeliveryPayslip),
                generalTax = null,
                status = IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
            )

            val thirdSpaceIncome = income(
                title = "third space delivery",
                workspace = planetExpressWorkspace,
                currency = "ZZA",
                originalAmount = 200,
                convertedAmounts = emptyAmountsInDefaultCurrency(),
                incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                generalTax = null,
                status = IncomeStatus.PENDING_CONVERSION
            )

            fun defaultNewIncome(): String = """{
                    "category": ${spaceDeliveryCategory.id},
                    "title": "new income",
                    "currency": "USD",
                    "originalAmount": 30000,
                    "convertedAmountInDefaultCurrency": 42000,
                    "incomeTaxableAmountInDefaultCurrency": 43000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "attachments": [${spaceDeliveryPayslip.id}],
                    "notes": "space delivery",
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
        }
    }
}
