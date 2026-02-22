package io.orangebuffalo.simpleaccounting.business.expenses

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

@DisplayName("Expenses API")
internal class ExpensesApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return expenses of a workspace of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 4)
                putJsonArray("data") {
                    addJsonObject {
                        put("category", preconditions.slurmCategory.id)
                        put("title", "best ever slurm")
                        put("currency", "THF")
                        put("originalAmount", 5000)
                        putJsonObject("convertedAmounts") {
                            put("originalAmountInDefaultCurrency", 500)
                            put("adjustedAmountInDefaultCurrency", 500)
                        }
                        putJsonObject("incomeTaxableAmounts") {
                            put("originalAmountInDefaultCurrency", 500)
                            put("adjustedAmountInDefaultCurrency", 500)
                        }
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        putJsonArray("attachments") {}
                        put("percentOnBusiness", 100)
                        put("id", preconditions.firstSlurm.id)
                        put("version", 0)
                        put("datePaid", MOCK_DATE_VALUE)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("status", "FINALIZED")
                    }
                    addJsonObject {
                        put("category", preconditions.slurmCategory.id)
                        put("title", "another great slurm")
                        put("currency", "ZZB")
                        put("originalAmount", 5100)
                        putJsonObject("convertedAmounts") {
                            put("originalAmountInDefaultCurrency", 510)
                            put("adjustedAmountInDefaultCurrency", 505)
                        }
                        putJsonObject("incomeTaxableAmounts") {
                            put("originalAmountInDefaultCurrency", 460)
                            put("adjustedAmountInDefaultCurrency", 455)
                        }
                        put("useDifferentExchangeRateForIncomeTaxPurposes", true)
                        putJsonArray("attachments") { add(preconditions.slurmReceipt.id) }
                        put("notes", "nice!")
                        put("percentOnBusiness", 99)
                        put("id", preconditions.secondSlurm.id)
                        put("version", 0)
                        put("datePaid", MOCK_DATE_VALUE)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("status", "FINALIZED")
                    }
                    addJsonObject {
                        put("category", preconditions.slurmCategory.id)
                        put("title", "slurm is never enough")
                        put("currency", "ZZB")
                        put("originalAmount", 5100)
                        putJsonObject("convertedAmounts") {
                            put("originalAmountInDefaultCurrency", 510)
                            put("adjustedAmountInDefaultCurrency", 459)
                        }
                        putJsonObject("incomeTaxableAmounts") {}
                        put("useDifferentExchangeRateForIncomeTaxPurposes", true)
                        putJsonArray("attachments") {}
                        put("percentOnBusiness", 99)
                        put("id", preconditions.thirdSlurm.id)
                        put("version", 0)
                        put("datePaid", MOCK_DATE_VALUE)
                        put("timeRecorded", MOCK_TIME_VALUE)
                        put("status", "PENDING_CONVERSION_FOR_TAXATION_PURPOSES")
                        put("generalTax", preconditions.slurmTax.id)
                        put("generalTaxRateInBps", 1000)
                        put("generalTaxAmount", 46)
                    }
                    addJsonObject {
                        put("category", preconditions.slurmCategory.id)
                        put("title", "need more slurm")
                        put("currency", "ZZB")
                        put("originalAmount", 5100)
                        putJsonObject("convertedAmounts") {}
                        putJsonObject("incomeTaxableAmounts") {}
                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                        putJsonArray("attachments") {}
                        put("percentOnBusiness", 100)
                        put("id", preconditions.fourthSlurm.id)
                        put("version", 0)
                        put("datePaid", MOCK_DATE_VALUE)
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
            .uri("/api/workspaces/27347947239/expenses")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for an expense only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return expense by id for current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.slurmCategory.id},
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
                    id: ${preconditions.firstSlurm.id},
                    version: 0,
                    datePaid: "$MOCK_DATE_VALUE",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "FINALIZED"
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting expense by id`() {
        client.get()
            .uri("/api/workspaces/5634632/expenses/${preconditions.firstSlurm.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting expense by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if expense belongs to another workspace when requesting expense by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.coffeeExpense.id}")
            .verifyNotFound("Expense ${preconditions.coffeeExpense.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating expense`() {
        client.post()
            .uri("/api/workspaces/995943/expenses")
            .sendJson(preconditions.defaultNewExpense())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new expense`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${preconditions.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "convertedAmountInDefaultCurrency": 42000,
                    "incomeTaxableAmountInDefaultCurrency": 41500,
                    "attachments": [${preconditions.slurmReceipt.id}],
                    "useDifferentExchangeRateForIncomeTaxPurposes": true,
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "generalTax": ${preconditions.slurmTax.id}
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.slurmCategory.id},
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
                    attachments: [${preconditions.slurmReceipt.id}],
                    notes: "coffee",
                    percentOnBusiness: 100,
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    datePaid: "$MOCK_DATE_VALUE",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "FINALIZED",
                    generalTax: ${preconditions.slurmTax.id},
                    generalTaxRateInBps: 1000,
                    generalTaxAmount: 3773
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating expense`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(preconditions.defaultNewExpense())
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new expense with minimum data for default currency`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${preconditions.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.slurmCategory.id},
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
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    datePaid: "$MOCK_DATE_VALUE",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    attachments: [],
                    status: "FINALIZED"
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when category of new expense is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
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
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${preconditions.coffeeCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Category ${preconditions.coffeeCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when tax of new expense is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${preconditions.slurmCategory.id},
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
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "category": ${preconditions.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "generalTax": ${preconditions.coffeeTax.id},
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Tax ${preconditions.coffeeTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 when attachment of new expense is not found`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
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
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses")
            .sendJson(
                """{
                    "attachments": [${preconditions.coffeeReceipt.id}],
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Documents [${preconditions.coffeeReceipt.id}] are not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update expense of current user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .sendJson(
                """{
                    "category": ${preconditions.beerCategory.id},
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
                    "generalTax": ${preconditions.slurmTax.id}
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    category: ${preconditions.beerCategory.id},
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
                    id: ${preconditions.firstSlurm.id},
                    version: 1,
                    datePaid: "3000-02-02",
                    timeRecorded: "$MOCK_TIME_VALUE",
                    status: "FINALIZED",
                    generalTax: ${preconditions.slurmTax.id},
                    generalTaxRateInBps: 1000,
                    generalTaxAmount: 3273
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should update expense of current user with minimum data`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .sendJson(
                """{
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "slurm updated",
                    currency: "HHD",
                    originalAmount: 20000,
                    attachments: [],
                    id: ${preconditions.firstSlurm.id},
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
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail with 404 on PUT when workspace belongs to another user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .sendJson(
                """{
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Workspace ${preconditions.fryWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when expense belongs to another workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.coffeeExpense.id}")
            .sendJson(
                """{
                    "title": "coffee updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Expense ${preconditions.coffeeExpense.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when expense does not exist`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/5566")
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
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
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
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .sendJson(
                """{
                    "category": ${preconditions.coffeeCategory.id},
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Category ${preconditions.coffeeCategory.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when tax is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
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
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .sendJson(
                """{
                    "generalTax": ${preconditions.coffeeTax.id},
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Tax ${preconditions.coffeeTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should fail with 404 on PUT when attachment is not found`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
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
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/expenses/${preconditions.firstSlurm.id}")
            .sendJson(
                """{
                    "attachments": [${preconditions.coffeeReceipt.id}],
                    "title": "slurm updated",
                    "currency": "HHD",
                    "originalAmount": 20000,
                    "datePaid": "3000-02-02",
                    "useDifferentExchangeRateForIncomeTaxPurposes": false
                }"""
            )
            .verifyNotFound("Documents [${preconditions.coffeeReceipt.id}] are not found")
    }

    private val preconditions by lazyPreconditions {
        object {
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
}
