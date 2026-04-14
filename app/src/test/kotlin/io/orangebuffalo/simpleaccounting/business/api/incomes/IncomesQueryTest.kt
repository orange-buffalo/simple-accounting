package io.orangebuffalo.simpleaccounting.business.api.incomes

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Base64

private fun Income.encodeCursor(): String =
    Base64.getEncoder().encodeToString("${dateReceived.toEpochDay()}:${createdAt!!.toEpochMilli()}".toByteArray())

class IncomesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeIncomes() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val income1 = income(workspace = workspace, title = "Slurm delivery payment", createdAt = MOCK_TIME.plusSeconds(100))
            val income2 = income(workspace = workspace, title = "Robot repair fee", createdAt = MOCK_TIME.plusSeconds(200))
            val income3 = income(workspace = workspace, title = "Interplanetary cargo", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeIncomes() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 2) {
                        edges {
                            cursor
                            node { title }
                        }
                        pageInfo {
                            startCursor
                            endCursor
                            hasPreviousPage
                            hasNextPage
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", testData.income1.encodeCursor())
                                    put("node", buildJsonObject { put("title", "Slurm delivery payment") })
                                })
                                add(buildJsonObject {
                                    put("cursor", testData.income2.encodeCursor())
                                    put("node", buildJsonObject { put("title", "Robot repair fee") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", testData.income1.encodeCursor())
                                put("endCursor", testData.income2.encodeCursor())
                                put("hasPreviousPage", false)
                                put("hasNextPage", true)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return second page using after cursor`() {
            val testData = preconditions { threeIncomes() }
            val afterCursor = testData.income2.encodeCursor()
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10, after = afterCursor) {
                        edges {
                            cursor
                            node { title }
                        }
                        pageInfo {
                            startCursor
                            endCursor
                            hasPreviousPage
                            hasNextPage
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", testData.income3.encodeCursor())
                                    put("node", buildJsonObject { put("title", "Interplanetary cargo") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", testData.income3.encodeCursor())
                                put("endCursor", testData.income3.encodeCursor())
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty page when workspace has no incomes`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges { node { title } }
                        pageInfo { startCursor; endCursor; hasPreviousPage; hasNextPage }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", emptyIncomesConnection(totalCount = 0, hasPreviousPage = false, hasNextPage = false))
                    }
                )
        }

        @Test
        fun `should sort incomes by dateReceived descending then createdAt ascending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    income(workspace = it.workspace, title = "Slurm payment 1",
                        dateReceived = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(100))
                    income(workspace = it.workspace, title = "Planet Express income",
                        dateReceived = LocalDate.of(3025, 1, 25), createdAt = MOCK_TIME.plusSeconds(200))
                    income(workspace = it.workspace, title = "Slurm payment 2",
                        dateReceived = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges { node { title } }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                incomeEdge(title = "Planet Express income")
                                incomeEdge(title = "Slurm payment 1")
                                incomeEdge(title = "Slurm payment 2")
                            }
                        })
                    }
                )
        }

        @Test
        fun `should apply freeSearchText filter when paginating`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    income(workspace = it.workspace, title = "Slurm payment 1",
                        dateReceived = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(100))
                    income(workspace = it.workspace, title = "Planet Express delivery",
                        dateReceived = LocalDate.of(3025, 1, 15), createdAt = MOCK_TIME.plusSeconds(200))
                    income(workspace = it.workspace, title = "Slurm payment 2",
                        dateReceived = LocalDate.of(3025, 1, 10), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10, freeSearchText = "slurm") {
                        edges { node { title } }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                incomeEdge(title = "Slurm payment 1")
                                incomeEdge(title = "Slurm payment 2")
                            }
                            put("totalCount", 2)
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Income Fields")
    inner class IncomeFields {

        @Test
        fun `should return all income fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(
                        workspace = workspace,
                        title = "Planet Express delivery fee",
                        dateReceived = LocalDate.of(3025, 1, 15),
                        currency = "EUR",
                        originalAmount = 12345L,
                        convertedAmounts = AmountsInDefaultCurrency(10000L, 9800L),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(8000L, 7500L),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        notes = "Good news, everyone! Delivery payment received.",
                        status = IncomeStatus.FINALIZED,
                        generalTaxRateInBps = 2000,
                        generalTaxAmount = 1633L,
                        createdAt = MOCK_TIME,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node {
                                id
                                version
                                title
                                dateReceived
                                currency
                                originalAmount
                                convertedAmounts {
                                    originalAmountInDefaultCurrency
                                    adjustedAmountInDefaultCurrency
                                }
                                useDifferentExchangeRateForIncomeTaxPurposes
                                incomeTaxableAmounts {
                                    originalAmountInDefaultCurrency
                                    adjustedAmountInDefaultCurrency
                                }
                                notes
                                createdAt
                                status
                                generalTaxRateInBps
                                generalTaxAmount
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.income.id!!.toInt())
                                        put("version", testData.income.version!!.toInt())
                                        put("title", "Planet Express delivery fee")
                                        put("dateReceived", "3025-01-15")
                                        put("currency", "EUR")
                                        put("originalAmount", 12345L)
                                        put("convertedAmounts", buildJsonObject {
                                            put("originalAmountInDefaultCurrency", 10000L)
                                            put("adjustedAmountInDefaultCurrency", 9800L)
                                        })
                                        put("useDifferentExchangeRateForIncomeTaxPurposes", false)
                                        put("incomeTaxableAmounts", buildJsonObject {
                                            put("originalAmountInDefaultCurrency", 8000L)
                                            put("adjustedAmountInDefaultCurrency", 7500L)
                                        })
                                        put("notes", "Good news, everyone! Delivery payment received.")
                                        put("createdAt", MOCK_TIME_VALUE)
                                        put("status", "FINALIZED")
                                        put("generalTaxRateInBps", 2000)
                                        put("generalTaxAmount", 1633L)
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return null optional fields when not set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(
                        workspace = workspace,
                        title = "Moon cargo delivery",
                        notes = null,
                        generalTax = null,
                        generalTaxRateInBps = null,
                        generalTaxAmount = null,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node {
                                notes
                                generalTaxRateInBps
                                generalTaxAmount
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("notes", null as String?)
                                        put("generalTaxRateInBps", null as String?)
                                        put("generalTaxAmount", null as String?)
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return category as object`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val category = category(workspace = workspace, name = "Delivery")
                    val income = income(workspace = workspace, title = "Interplanetary delivery", category = category)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { category { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("category", buildJsonObject {
                                            put("id", testData.category.id!!.toInt())
                                            put("name", "Delivery")
                                        })
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return null category when not set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(workspace = workspace, title = "Slurm payment")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { category { id } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("category", null as String?)
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return generalTax as object`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val tax = generalTax(workspace = workspace, title = "VAT 10%", rateInBps = 1000)
                    val income = income(workspace = workspace, title = "Robot oil sales", generalTax = tax)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { generalTax { id; title; rateInBps } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("generalTax", buildJsonObject {
                                            put("id", testData.tax.id!!.toInt())
                                            put("title", "VAT 10%")
                                            put("rateInBps", 1000)
                                        })
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return null generalTax when not set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(workspace = workspace, title = "Slurm payment", generalTax = null)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { generalTax { id } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("generalTax", null as String?)
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return empty attachments when none are set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(workspace = workspace, title = "Slurm payment")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        putJsonArray("attachments") {}
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return single attachment`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val doc = document(workspace = workspace, name = "Slurm invoice")
                    val income = income(workspace = workspace, title = "Slurm payment", attachments = setOf(doc))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        putJsonArray("attachments") {
                                            add(buildJsonObject {
                                                put("id", testData.doc.id!!.toInt())
                                                put("name", "Slurm invoice")
                                            })
                                        }
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return linkedInvoice as object`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer = customer(workspace = workspace, name = "MomCorp")
                    val invoice = invoice(customer = customer, title = "Robot oil invoice")
                    val income = income(workspace = workspace, title = "Robot oil payment", linkedInvoice = invoice)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { linkedInvoice { id; title } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("linkedInvoice", buildJsonObject {
                                            put("id", testData.invoice.id!!.toInt())
                                            put("title", "Robot oil invoice")
                                        })
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return null linkedInvoice when not set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(workspace = workspace, title = "Slurm payment")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomes(first = 10) {
                        edges {
                            node { linkedInvoice { id } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("linkedInvoice", null as String?)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("income(id) query")
    inner class IncomeByIdQuery {

        @Test
        fun `should return income by id`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val income = income(workspace = workspace, title = "Slurm payment", originalAmount = 5000)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    income(id = testData.income.id!!) {
                        id
                        title
                        originalAmount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("income", buildJsonObject {
                            put("id", testData.income.id!!.toInt())
                            put("title", "Slurm payment")
                            put("originalAmount", 5000)
                        })
                    }
                )
        }

        @Test
        fun `should return null for non-existent income`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    income(id = Long.MAX_VALUE) {
                        id
                        title
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("income", null as String?)
                    }
                )
        }

        @Test
        fun `should return null for income in another workspace`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val fryWorkspace = workspace(owner = fry)
                    val zoidberg = zoidberg()
                    val zoidbergWorkspace = workspace(owner = zoidberg)
                    val zoidbergIncome = income(workspace = zoidbergWorkspace, title = "Zoidberg's payment")
                }
            }
            client.graphql {
                workspace(id = testData.fryWorkspace.id!!) {
                    income(id = testData.zoidbergIncome.id!!) {
                        id
                        title
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("income", null as String?)
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.incomeEdge(title: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("title", title) })
    })
}

private fun emptyIncomesConnection(
    totalCount: Int,
    hasPreviousPage: Boolean,
    hasNextPage: Boolean,
): JsonElement = buildJsonObject {
    putJsonArray("edges") {}
    put("pageInfo", buildJsonObject {
        put("startCursor", null as String?)
        put("endCursor", null as String?)
        put("hasPreviousPage", hasPreviousPage)
        put("hasNextPage", hasNextPage)
    })
    put("totalCount", totalCount)
}
