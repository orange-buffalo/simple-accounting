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
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Expenses API ")
internal class ExpensesApiControllerIT(
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
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return expenses of a workspace of current user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("4")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "best ever slurm",
                            currency: "THF",
                            originalAmount: 5000,
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 500,
                                adjustedAmountInDefaultCurrency: 500
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 500,
                                adjustedAmountInDefaultCurrency: 500
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 510,
                                adjustedAmountInDefaultCurrency: 505
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 460,
                                adjustedAmountInDefaultCurrency: 455
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: true,
                            attachments: [${testData.slurmReceipt.id}],
                            notes: "nice!",
                            percentOnBusiness: 99,
                            id: ${testData.secondSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    ),

                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "slurm is never enough",
                            currency: "ZZB",
                            originalAmount: 5100,
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 510,
                                adjustedAmountInDefaultCurrency: 459
                            },
                            incomeTaxableAmounts: {
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: true,
                            attachments: [],
                            percentOnBusiness: 99,
                            id: ${testData.thirdSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "PENDING_CONVERSION_FOR_TAXATION_PURPOSES",
                            generalTax: ${testData.slurmTax.id},
                            generalTaxRateInBps: 1000,
                            generalTaxAmount: 46
                    }"""
                    ),

                    json(
                        """{
                            category: ${testData.slurmCategory.id},
                            title: "need more slurm",
                            currency: "ZZB",
                            originalAmount: 5100,
                            convertedAmounts: {
                            },
                            incomeTaxableAmounts: {
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
                            attachments: [],
                            percentOnBusiness: 100,
                            id: ${testData.fourthSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
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
            .uri("/api/workspaces/27347947239/expenses")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an expense only for logged in users`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return expense by id for current user`() {
        val testData = setupPreconditions()
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 500,
                                adjustedAmountInDefaultCurrency: 500
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 500,
                                adjustedAmountInDefaultCurrency: 500
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
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
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting expense by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/5634632/expenses/${testData.firstSlurm.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting expense by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if expense belongs to another workspace when requesting expense by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.coffeeExpense.id}")
            .verifyNotFound("Expense ${testData.coffeeExpense.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating expense`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/995943/expenses")
            .sendJson(testData.defaultNewExpense())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new expense`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "convertedAmountInDefaultCurrency": 42000,
                    "incomeTaxableAmountInDefaultCurrency": 41500,
                    "attachments": [${testData.slurmReceipt.id}],
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "generalTax": ${testData.slurmTax.id}
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 42000,
                                adjustedAmountInDefaultCurrency: 38182
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 41500,
                                adjustedAmountInDefaultCurrency: 37727
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: true,
                            attachments: [${testData.slurmReceipt.id}],
                            notes: "coffee",
                            percentOnBusiness: 100,
                            id: "#{json-unit.any-number}",
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            generalTax: ${testData.slurmTax.id},
                            generalTaxRateInBps: 1000,
                            generalTaxAmount: 3773
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating expense`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(testData.defaultNewExpense())
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new expense with minimum data for default currency`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 150,
                                adjustedAmountInDefaultCurrency: 150
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 150,
                                adjustedAmountInDefaultCurrency: 150
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
                            percentOnBusiness: 100,
                            id: "#{json-unit.any-number}",
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
    @WithMockFryUser
    fun `should return 404 when category of new expense is not found`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": 537453,
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Category 537453 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when category of new expense belongs to another workspace`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${testData.coffeeCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Category ${testData.coffeeCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new expense is not found`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "generalTax": 4455,
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Tax 4455 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new expense belongs to another workspace`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${testData.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "generalTax": ${testData.coffeeTax.id},
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Tax ${testData.coffeeTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new expense is not found`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "attachments": [537453],
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Documents [537453] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new expense belongs to another workspace`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "attachments": [${testData.coffeeReceipt.id}],
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Documents [${testData.coffeeReceipt.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update expense of current user`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "category": ${testData.beerCategory.id},
                    "title": "slurm -> beer",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "convertedAmountInDefaultCurrency": 30000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "incomeTaxableAmountInDefaultCurrency": 40000,
                    "attachments": [],
                    "notes": "beer",
                    "percentOnBusiness": 90,
                    "datePaid": "3000-02-02",
                    "generalTax": ${testData.slurmTax.id}
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
                            convertedAmounts: {
                                originalAmountInDefaultCurrency: 30000,
                                adjustedAmountInDefaultCurrency: 24545
                            },
                            incomeTaxableAmounts: {
                                originalAmountInDefaultCurrency: 40000,
                                adjustedAmountInDefaultCurrency: 32727
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: true,
                            attachments: [],
                            notes: "beer",
                            percentOnBusiness: 90,
                            id: ${testData.firstSlurm.id},
                            version: 1,
                            datePaid: "3000-02-02",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED",
                            generalTax: ${testData.slurmTax.id},
                            generalTaxRateInBps: 1000,
                            generalTaxAmount: 3273
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should update expense of current user with minimum data`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
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
                            convertedAmounts: {
                            },
                            incomeTaxableAmounts: {
                            },
                            useDifferentExchangeRateForIncomeTaxPurposes: false,
                            percentOnBusiness: 100
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
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Workspace ${testData.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when expense belongs to another workspace`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.coffeeExpense.id}")
            .sendJson(
                """{
                    "title": "coffee updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Expense ${testData.coffeeExpense.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when expense does not exist`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/5566")
            .sendJson(
                """{
                    "title": "coffee updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Expense 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when category is not found`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "category": 5566,
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Category 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when category belongs to another workspace`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "category": ${testData.coffeeCategory.id},
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Category ${testData.coffeeCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax is not found`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "generalTax": 5566,
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Tax 5566 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax belongs to another workspace`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "generalTax": ${testData.coffeeTax.id},
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Tax ${testData.coffeeTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "attachments": [5566],
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Documents [5566] are not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment belongs to another workspace`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/expenses/${testData.firstSlurm.id}")
            .sendJson(
                """{
                    "attachments": [${testData.coffeeReceipt.id}],
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Documents [${testData.coffeeReceipt.id}] are not found")
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()
        val farnsworth = farnsworth()
        val fryWorkspace = workspace(owner = fry)
        val fryCoffeeWorkspace = workspace(owner = fry)
        val coffeeCategory = category(workspace = fryCoffeeWorkspace)
        val slurmCategory = category(workspace = fryWorkspace)
        val beerCategory = category(workspace = fryWorkspace)
        val coffeeTax = generalTax(workspace = fryCoffeeWorkspace)
        val slurmTax = generalTax(workspace = fryWorkspace)
        val slurmReceipt = document(workspace = fryWorkspace)
        val coffeeReceipt = document(workspace = fryCoffeeWorkspace)

        val coffeeExpense = expense(
            workspace = fryCoffeeWorkspace,
            category = coffeeCategory,
            currency = "THF",
            originalAmount = 50,
            convertedAmounts = amountsInDefaultCurrency(50),
            incomeTaxableAmounts = amountsInDefaultCurrency(50),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            status = ExpenseStatus.FINALIZED,
            percentOnBusiness = 100
        )

        val firstSlurm = expense(
            title = "best ever slurm",
            workspace = fryWorkspace,
            category = slurmCategory,
            currency = "THF",
            originalAmount = 5000,
            convertedAmounts = amountsInDefaultCurrency(500),
            incomeTaxableAmounts = amountsInDefaultCurrency(500),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            status = ExpenseStatus.FINALIZED
        )

        val secondSlurm = expense(
            title = "another great slurm",
            workspace = fryWorkspace,
            category = slurmCategory,
            currency = "ZZB",
            originalAmount = 5100,
            convertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 510,
                adjustedAmountInDefaultCurrency = 505
            ),
            incomeTaxableAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 460,
                adjustedAmountInDefaultCurrency = 455
            ),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            status = ExpenseStatus.FINALIZED,
            percentOnBusiness = 99,
            notes = "nice!",
            attachments = setOf(slurmReceipt),
            generalTax = null
        )

        val thirdSlurm = expense(
            title = "slurm is never enough",
            workspace = fryWorkspace,
            category = slurmCategory,
            currency = "ZZB",
            originalAmount = 5100,
            convertedAmounts = AmountsInDefaultCurrency(
                originalAmountInDefaultCurrency = 510,
                adjustedAmountInDefaultCurrency = 459
            ),
            incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
            useDifferentExchangeRateForIncomeTaxPurposes = true,
            status = ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES,
            percentOnBusiness = 99,
            generalTax = slurmTax,
            generalTaxRateInBps = 10_00,
            generalTaxAmount = 46
        )

        val fourthSlurm = expense(
            title = "need more slurm",
            workspace = fryWorkspace,
            category = slurmCategory,
            currency = "ZZB",
            originalAmount = 5100,
            convertedAmounts = emptyAmountsInDefaultCurrency(),
            incomeTaxableAmounts = emptyAmountsInDefaultCurrency(),
            useDifferentExchangeRateForIncomeTaxPurposes = false,
            status = ExpenseStatus.PENDING_CONVERSION,
            percentOnBusiness = 100
        )

        fun defaultNewExpense(): String = """{
                    "category": ${slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 30000,
                    "convertedAmountInDefaultCurrency": 42000,
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "incomeTaxableAmountInDefaultCurrency": 41500,
                    "attachments": [${slurmReceipt.id}],
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
    }
}
