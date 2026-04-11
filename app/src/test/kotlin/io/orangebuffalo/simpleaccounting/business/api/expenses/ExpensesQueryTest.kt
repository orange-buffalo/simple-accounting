package io.orangebuffalo.simpleaccounting.business.api.expenses

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
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

class ExpensesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeExpenses() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val expense1 = expense(workspace = workspace, title = "Slurm supplies", createdAt = MOCK_TIME.plusSeconds(100))
            val expense2 = expense(workspace = workspace, title = "Robot oil", createdAt = MOCK_TIME.plusSeconds(200))
            val expense3 = expense(workspace = workspace, title = "Spaceship parts", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeExpenses() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 2) {
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
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeExpenseCursor(testData.expense1.datePaid, testData.expense1.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Slurm supplies") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeExpenseCursor(testData.expense2.datePaid, testData.expense2.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Robot oil") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeExpenseCursor(testData.expense1.datePaid, testData.expense1.createdAt!!))
                                put("endCursor", encodeExpenseCursor(testData.expense2.datePaid, testData.expense2.createdAt!!))
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
            val testData = preconditions { threeExpenses() }
            val afterCursor = encodeExpenseCursor(testData.expense2.datePaid, testData.expense2.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, after = afterCursor) {
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
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeExpenseCursor(testData.expense3.datePaid, testData.expense3.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Spaceship parts") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeExpenseCursor(testData.expense3.datePaid, testData.expense3.createdAt!!))
                                put("endCursor", encodeExpenseCursor(testData.expense3.datePaid, testData.expense3.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no expenses exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
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
                        put("expenses", emptyExpensesConnection(
                            hasPreviousPage = false,
                            hasNextPage = false,
                            totalCount = 0,
                        ))
                    }
                )
        }

        @Test
        fun `should return empty page when cursor is past all items`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val expense = expense(workspace = workspace, title = "Slurm supplies", createdAt = MOCK_TIME.plusSeconds(100))
                }
            }
            val afterCursor = encodeExpenseCursor(testData.expense.datePaid, testData.expense.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, after = afterCursor) {
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
                        put("expenses", emptyExpensesConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should not include expenses from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val expense = expense(workspace = workspace, title = "Slurm supplies", createdAt = MOCK_TIME.plusSeconds(100))
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    expense(workspace = otherWorkspace, title = "Robot oil")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Slurm supplies")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should order by datePaid descending then createdAt ascending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    expense(workspace = it.workspace, title = "Slurm supplies",
                        datePaid = LocalDate.of(3025, 1, 10), createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Robot oil",
                        datePaid = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(200))
                    expense(workspace = it.workspace, title = "Spaceship parts",
                        datePaid = LocalDate.of(3025, 1, 15), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { title }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Robot oil")
                                expenseEdge(title = "Spaceship parts")
                                expenseEdge(title = "Slurm supplies")
                            }
                        })
                    }
                )
        }

        @Test
        fun `should sort by createdAt ascending as secondary sort when datePaid is equal`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    expense(workspace = it.workspace, title = "Slurm supplies",
                        datePaid = MOCK_DATE, createdAt = MOCK_TIME.plusSeconds(300))
                    expense(workspace = it.workspace, title = "Robot oil",
                        datePaid = MOCK_DATE, createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Spaceship parts",
                        datePaid = MOCK_DATE, createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { title }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Robot oil")
                                expenseEdge(title = "Spaceship parts")
                                expenseEdge(title = "Slurm supplies")
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Filtering")
    inner class Filtering {

        @Test
        fun `should filter by title`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    expense(workspace = it.workspace, title = "Slurm supplies",
                        datePaid = LocalDate.of(3025, 1, 10), createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Robot oil",
                        datePaid = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(200))
                    expense(workspace = it.workspace, title = "slurm machine",
                        datePaid = LocalDate.of(3025, 1, 15), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, freeSearchText = "slurm") {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "slurm machine")
                                expenseEdge(title = "Slurm supplies")
                            }
                            put("totalCount", 2)
                        })
                    }
                )
        }

        @Test
        fun `should filter by notes`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    expense(workspace = it.workspace, title = "Robot oil",
                        notes = "Delivery to Omicron Persei 8", createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Spaceship parts",
                        notes = "Planet Express maintenance", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, freeSearchText = "omicron") {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Robot oil")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should filter by category name`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val deliveryCategory = category(workspace = workspace, name = "Delivery")
                    val maintenanceCategory = category(workspace = workspace, name = "Maintenance")
                }.also {
                    expense(workspace = it.workspace, title = "Slurm supplies",
                        category = it.deliveryCategory, createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Robot oil",
                        category = it.maintenanceCategory, createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, freeSearchText = "delivery") {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Slurm supplies")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should return all expenses when freeSearchText is not provided`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    expense(workspace = it.workspace, title = "Slurm supplies", createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Robot oil", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Slurm supplies")
                                expenseEdge(title = "Robot oil")
                            }
                            put("totalCount", 2)
                        })
                    }
                )
        }

        @Test
        fun `should return empty result when no expenses match search`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    expense(workspace = it.workspace, title = "Slurm supplies", createdAt = MOCK_TIME.plusSeconds(100))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, freeSearchText = "bender") {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {}
                            put("totalCount", 0)
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
                    expense(workspace = it.workspace, title = "Slurm supplies 1",
                        datePaid = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(100))
                    expense(workspace = it.workspace, title = "Robot oil",
                        datePaid = LocalDate.of(3025, 1, 15), createdAt = MOCK_TIME.plusSeconds(200))
                    expense(workspace = it.workspace, title = "Slurm supplies 2",
                        datePaid = LocalDate.of(3025, 1, 10), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10, freeSearchText = "slurm") {
                        edges {
                            node { title }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                expenseEdge(title = "Slurm supplies 1")
                                expenseEdge(title = "Slurm supplies 2")
                            }
                            put("totalCount", 2)
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Expense Fields")
    inner class ExpenseFields {

        @Test
        fun `should return all expense fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val expense = expense(
                        workspace = workspace,
                        title = "Slurm delivery invoice",
                        datePaid = LocalDate.of(3025, 1, 15),
                        currency = "EUR",
                        originalAmount = 12345L,
                        convertedAmounts = AmountsInDefaultCurrency(10000L, 9800L),
                        incomeTaxableAmounts = AmountsInDefaultCurrency(8000L, 7500L),
                        useDifferentExchangeRateForIncomeTaxPurposes = false,
                        percentOnBusiness = 80,
                        notes = "Good news, everyone! Delivery complete.",
                        status = ExpenseStatus.FINALIZED,
                        generalTaxRateInBps = 2000,
                        generalTaxAmount = 1633L,
                        createdAt = MOCK_TIME,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node {
                                id
                                version
                                title
                                datePaid
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
                                percentOnBusiness
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
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.expense.id!!.toInt())
                                        put("version", testData.expense.version!!.toInt())
                                        put("title", "Slurm delivery invoice")
                                        put("datePaid", "3025-01-15")
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
                                        put("percentOnBusiness", 80)
                                        put("notes", "Good news, everyone! Delivery complete.")
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
                    val expense = expense(
                        workspace = workspace,
                        title = "Planet Express equipment",
                        notes = null,
                        generalTax = null,
                        generalTaxRateInBps = null,
                        generalTaxAmount = null,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node {
                                notes
                                generalTaxId
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
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("notes", null as String?)
                                        put("generalTaxId", null as String?)
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
                    val expense = expense(workspace = workspace, title = "Slurm supplies", category = category)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { category { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
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
                    val expense = expense(workspace = workspace, title = "Slurm supplies")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { category { id } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
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
        fun `should return empty attachments when none are set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val expense = expense(workspace = workspace, title = "Slurm supplies")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
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
                    val doc = document(workspace = workspace, name = "Slurm delivery receipt")
                    val expense = expense(workspace = workspace, title = "Slurm supplies", attachments = setOf(doc))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        putJsonArray("attachments") {
                                            add(buildJsonObject {
                                                put("id", testData.doc.id!!.toInt())
                                                put("name", "Slurm delivery receipt")
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
        fun `should return multiple attachments`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val doc1 = document(workspace = workspace, name = "Robot oil receipt")
                    val doc2 = document(workspace = workspace, name = "Slurm delivery receipt")
                    val expense = expense(
                        workspace = workspace,
                        title = "Planet Express supplies",
                        attachments = setOf(doc1, doc2),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    expenses(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("expenses", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        putJsonArray("attachments") {
                                            add(buildJsonObject {
                                                put("id", testData.doc1.id!!.toInt())
                                                put("name", "Robot oil receipt")
                                            })
                                            add(buildJsonObject {
                                                put("id", testData.doc2.id!!.toInt())
                                                put("name", "Slurm delivery receipt")
                                            })
                                        }
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.expenseEdge(title: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("title", title) })
    })
}

private fun emptyExpensesConnection(
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
