package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE_VALUE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME_VALUE
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeStatus
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Incomes API ")
internal class IncomesApiControllerIT(
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
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return incomes of a workspace of current user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("3")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
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
                            id: ${testData.firstSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            generalTax: ${testData.spaceTax.id},
                            generalTaxAmount: 20,
                            generalTaxRateInBps: 12000,
                            linkedInvoice: ${testData.firstSpaceInvoice.id}
                    }"""
                    ),

                    json(
                        """{
                            category: ${testData.spaceDeliveryCategory.id},
                            title: "second space delivery",
                            currency: "ZZB",
                            originalAmount: 5100,
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 510,
                                adjustedAmountInDefaultCurrency: 510
                            },
                            incomeTaxableAmounts: {
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: true,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            notes: "nice!",
                            id: ${testData.secondSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PENDING_CONVERSION_FOR_TAXATION_PURPOSES"
                    }"""
                    ),

                    json(
                        """{
                            title: "third space delivery",
                            currency: "ZZA",
                            originalAmount: 200,
                            convertedAmounts: {
                            },
                            incomeTaxableAmounts: {
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
                            attachments: [],
                            id: ${testData.thirdSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PENDING_CONVERSION"
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
            .uri("/api/workspaces/27347947239/incomes")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an income only for logged in users`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/expenses/${testData.firstSpaceIncome.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return income by id for current user`() {
        val testData = setupPreconditions()
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
                            id: ${testData.firstSpaceIncome.id},
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            generalTax: ${testData.spaceTax.id},
                            generalTaxAmount: 20,
                            generalTaxRateInBps: 12000,
                            linkedInvoice: ${testData.firstSpaceInvoice.id}
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting income by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/5634632/incomes/${testData.firstSpaceIncome.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting income by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if income belongs to another workspace when requesting income by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.pizzaWageIncome.id}")
            .verifyNotFound("Income ${testData.pizzaWageIncome.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating income`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/995943/incomes")
            .sendJson(testData.defaultNewIncome())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new space delivery",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "convertedAmountInDefaultCurrency": 42000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "incomeTaxableAmountInDefaultCurrency": 37727,
                    "attachments": [${testData.spaceDeliveryPayslip.id}],
                    "notes": "delivery",
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "generalTax": ${testData.planetExpressTax.id}
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 42000,
                                adjustedAmountInDefaultCurrency: 38182
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 37727,
                                adjustedAmountInDefaultCurrency: 34297
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: true,
                            attachments: [${testData.spaceDeliveryPayslip.id}],
                            notes: "delivery",
                            id: "#{json-unit.any-number}",
                            version: 0,
                            dateReceived: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            generalTax: ${testData.planetExpressTax.id},
                            generalTaxRateInBps: 1000,
                            generalTaxAmount: 3430
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating income`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(testData.defaultNewIncome())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new income with minimum data for default currency`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 150,
                                adjustedAmountInDefaultCurrency: 150
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 150,
                                adjustedAmountInDefaultCurrency: 150
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
                            id: "#{json-unit.any-number}",
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
    @WithMockFryUser
    fun `should return 404 when category of new income is not found`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
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
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.pizzaCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE"
                }"""
            )
            .verifyNotFound("Category ${testData.pizzaCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new income is not found`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
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
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "category": ${testData.spaceDeliveryCategory.id},
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "generalTax": ${testData.pizzaDeliveryTax.id}
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new income is not found`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
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
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes")
            .sendJson(
                """{
                    "title": "new space income",
                    "currency": "USD",
                    "originalAmount": 150,
                    "convertedAmountInDefaultCurrency": 150,
                    "incomeTaxableAmountInDefaultCurrency": 150,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "$MOCK_DATE_VALUE",
                    "attachments": [${testData.pizzaDeliveryPayslip.id}]
                }"""
            )
            .verifyNotFound("Documents [${testData.pizzaDeliveryPayslip.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update income of current user`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": ${testData.pensionCategory.id},
                    "title": "space -> pension",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "convertedAmountInDefaultCurrency": 30000,
                    "incomeTaxableAmountInDefaultCurrency": 32727,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "attachments": [],
                    "notes": "pension",
                    "dateReceived": "3000-02-02",
                    "generalTax": ${testData.planetExpressTax.id},
                    "linkedInvoice": ${testData.secondSpaceInvoice.id}
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
                            id: ${testData.firstSpaceIncome.id},
                            version: 1,
                            dateReceived: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            generalTax: ${testData.planetExpressTax.id},
                            generalTaxRateInBps: 1000,
                            generalTaxAmount: 2975,
                            linkedInvoice: ${testData.secondSpaceInvoice.id}
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should update income of current user with minimum data`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "title": "delivery updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
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
                            convertedAmounts: {
                            },
                            incomeTaxableAmounts: {
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false
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
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "title": "delivery updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income belongs to another workspace`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.pizzaWageIncome.id}")
            .sendJson(
                """{
                    "title": "pizza updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Income ${testData.pizzaWageIncome.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when income does not exist`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/5566")
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
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
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
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "category": ${testData.pizzaCategory.id},
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Category ${testData.pizzaCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax is not found`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
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
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "generalTax": ${testData.pizzaDeliveryTax.id},
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Tax ${testData.pizzaDeliveryTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
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
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/incomes/${testData.firstSpaceIncome.id}")
            .sendJson(
                """{
                    "attachments": [${testData.pizzaDeliveryPayslip.id}],
                    "title": "income updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false,
                    "dateReceived": "3000-02-02"
                }"""
            )
            .verifyNotFound("Documents [${testData.pizzaDeliveryPayslip.id}] are not found")
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
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
