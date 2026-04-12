package io.orangebuffalo.simpleaccounting.business.api.invoices

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
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

private fun Invoice.encodeCursor(): String =
    Base64.getEncoder().encodeToString("${dateIssued.toEpochDay()}:${createdAt!!.toEpochMilli()}".toByteArray())

class InvoicesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeInvoices() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val customer = customer(workspace = workspace, name = "MomCorp")
            val invoice1 = invoice(customer = customer, title = "Slurm delivery", createdAt = MOCK_TIME.plusSeconds(100))
            val invoice2 = invoice(customer = customer, title = "Robot maintenance", createdAt = MOCK_TIME.plusSeconds(200))
            val invoice3 = invoice(customer = customer, title = "Spaceship parts", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeInvoices() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 2) {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", testData.invoice1.encodeCursor())
                                    put("node", buildJsonObject { put("title", "Slurm delivery") })
                                })
                                add(buildJsonObject {
                                    put("cursor", testData.invoice2.encodeCursor())
                                    put("node", buildJsonObject { put("title", "Robot maintenance") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", testData.invoice1.encodeCursor())
                                put("endCursor", testData.invoice2.encodeCursor())
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
            val testData = preconditions { threeInvoices() }
            val afterCursor = testData.invoice2.encodeCursor()
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10, after = afterCursor) {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", testData.invoice3.encodeCursor())
                                    put("node", buildJsonObject { put("title", "Spaceship parts") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", testData.invoice3.encodeCursor())
                                put("endCursor", testData.invoice3.encodeCursor())
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no invoices exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
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
                        put("invoices", emptyInvoicesConnection(
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
                    val invoice = invoice(
                        customer = customer(workspace = workspace),
                        title = "Slurm delivery",
                        createdAt = MOCK_TIME.plusSeconds(100),
                    )
                }
            }
            val afterCursor = testData.invoice.encodeCursor()
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10, after = afterCursor) {
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
                        put("invoices", emptyInvoicesConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should not include invoices from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val invoice = invoice(
                        customer = customer(workspace = workspace),
                        title = "Slurm delivery",
                        createdAt = MOCK_TIME.plusSeconds(100),
                    )
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    invoice(customer = customer(workspace = otherWorkspace), title = "Robot oil")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Slurm delivery")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should order by dateIssued descending then createdAt ascending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer = customer(workspace = workspace)
                }.also {
                    invoice(customer = it.customer, title = "Slurm delivery",
                        dateIssued = LocalDate.of(3025, 1, 10), createdAt = MOCK_TIME.plusSeconds(100))
                    invoice(customer = it.customer, title = "Robot maintenance",
                        dateIssued = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(200))
                    invoice(customer = it.customer, title = "Spaceship parts",
                        dateIssued = LocalDate.of(3025, 1, 15), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
                        edges {
                            node { title }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Robot maintenance")
                                invoiceEdge(title = "Spaceship parts")
                                invoiceEdge(title = "Slurm delivery")
                            }
                        })
                    }
                )
        }

        @Test
        fun `should sort by createdAt ascending as secondary sort when dateIssued is equal`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer = customer(workspace = workspace)
                }.also {
                    invoice(customer = it.customer, title = "Slurm delivery",
                        dateIssued = MOCK_DATE, createdAt = MOCK_TIME.plusSeconds(300))
                    invoice(customer = it.customer, title = "Robot maintenance",
                        dateIssued = MOCK_DATE, createdAt = MOCK_TIME.plusSeconds(100))
                    invoice(customer = it.customer, title = "Spaceship parts",
                        dateIssued = MOCK_DATE, createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
                        edges {
                            node { title }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Robot maintenance")
                                invoiceEdge(title = "Spaceship parts")
                                invoiceEdge(title = "Slurm delivery")
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
                    val customer = customer(workspace = workspace)
                }.also {
                    invoice(customer = it.customer, title = "Slurm delivery",
                        dateIssued = LocalDate.of(3025, 1, 10), createdAt = MOCK_TIME.plusSeconds(100))
                    invoice(customer = it.customer, title = "Robot maintenance",
                        dateIssued = LocalDate.of(3025, 1, 20), createdAt = MOCK_TIME.plusSeconds(200))
                    invoice(customer = it.customer, title = "Slurm resupply",
                        dateIssued = LocalDate.of(3025, 1, 15), createdAt = MOCK_TIME.plusSeconds(300))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10, freeSearchText = "slurm") {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Slurm resupply")
                                invoiceEdge(title = "Slurm delivery")
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
                    val customer = customer(workspace = workspace)
                }.also {
                    invoice(customer = it.customer, title = "Robot maintenance",
                        notes = "Delivery to Omicron Persei 8", createdAt = MOCK_TIME.plusSeconds(100))
                    invoice(customer = it.customer, title = "Spaceship parts",
                        notes = "Planet Express maintenance", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10, freeSearchText = "omicron") {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Robot maintenance")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should filter by customer name`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val momCorp = customer(workspace = workspace, name = "MomCorp")
                    val planetExpress = customer(workspace = workspace, name = "Planet Express")
                }.also {
                    invoice(customer = it.momCorp, title = "Slurm delivery",
                        createdAt = MOCK_TIME.plusSeconds(100))
                    invoice(customer = it.planetExpress, title = "Robot maintenance",
                        createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10, freeSearchText = "MomCorp") {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Slurm delivery")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should return all invoices when freeSearchText is not provided`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer = customer(workspace = workspace)
                }.also {
                    invoice(customer = it.customer, title = "Slurm delivery", createdAt = MOCK_TIME.plusSeconds(100))
                    invoice(customer = it.customer, title = "Robot maintenance", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
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
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                invoiceEdge(title = "Slurm delivery")
                                invoiceEdge(title = "Robot maintenance")
                            }
                            put("totalCount", 2)
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Invoice Fields")
    inner class InvoiceFields {

        @Test
        fun `should return all invoice fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer = customer(workspace = workspace, name = "MomCorp")
                    val generalTax = generalTax(workspace = workspace, rateInBps = 1000)
                    val document = document(workspace = workspace)
                    val invoice = invoice(
                        customer = customer,
                        title = "Slurm delivery invoice",
                        dateIssued = LocalDate.of(3025, 1, 15),
                        dateSent = LocalDate.of(3025, 1, 16),
                        datePaid = LocalDate.of(3025, 2, 1),
                        dueDate = LocalDate.of(3025, 1, 30),
                        currency = "EUR",
                        amount = 12345L,
                        notes = "Good news, everyone! Delivery complete.",
                        generalTax = generalTax,
                        attachments = setOf(document),
                        createdAt = MOCK_TIME,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
                        edges {
                            node {
                                id
                                version
                                title
                                dateIssued
                                dateSent
                                datePaid
                                timeCancelled
                                dueDate
                                currency
                                amount
                                notes
                                createdAt
                                status
                                generalTaxId
                                customer { id; name }
                                attachments { id }
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.invoice.id!!.toInt())
                                        put("version", testData.invoice.version!!.toInt())
                                        put("title", "Slurm delivery invoice")
                                        put("dateIssued", "3025-01-15")
                                        put("dateSent", "3025-01-16")
                                        put("datePaid", "3025-02-01")
                                        put("timeCancelled", null as String?)
                                        put("dueDate", "3025-01-30")
                                        put("currency", "EUR")
                                        put("amount", 12345L)
                                        put("notes", "Good news, everyone! Delivery complete.")
                                        put("createdAt", MOCK_TIME_VALUE)
                                        put("status", "DRAFT")
                                        put("generalTaxId", testData.generalTax.id!!.toInt())
                                        put("customer", buildJsonObject {
                                            put("id", testData.customer.id!!.toInt())
                                            put("name", "MomCorp")
                                        })
                                        putJsonArray("attachments") {
                                            add(buildJsonObject {
                                                put("id", testData.document.id!!.toInt())
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
        fun `should return null optional fields when not set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val invoice = invoice(
                        customer = customer(workspace = workspace),
                        title = "Planet Express equipment",
                        notes = null,
                        generalTax = null,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoices(first = 10) {
                        edges {
                            node {
                                notes
                                generalTaxId
                                dateSent
                                datePaid
                                timeCancelled
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoices", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("notes", null as String?)
                                        put("generalTaxId", null as String?)
                                        put("dateSent", null as String?)
                                        put("datePaid", null as String?)
                                        put("timeCancelled", null as String?)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Invoice by ID")
    inner class InvoiceById {

        @Test
        fun `should return invoice by ID`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val invoice = invoice(
                        customer = customer(workspace = workspace),
                        title = "Slurm delivery",
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoice(id = testData.invoice.id!!) {
                        title
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoice", buildJsonObject {
                            put("title", "Slurm delivery")
                        })
                    }
                )
        }

        @Test
        fun `should return null for non-existent invoice`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoice(id = Long.MAX_VALUE) {
                        title
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoice", null as String?)
                    }
                )
        }

        @Test
        fun `should return null for invoice from another workspace`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val otherWorkspace = workspace(owner = zoidberg())
                    val otherInvoice = invoice(
                        customer = customer(workspace = otherWorkspace),
                        title = "Robot oil",
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    invoice(id = testData.otherInvoice.id!!) {
                        title
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("invoice", null as String?)
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.invoiceEdge(title: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("title", title) })
    })
}

private fun emptyInvoicesConnection(
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
