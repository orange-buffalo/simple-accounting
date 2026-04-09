package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class DocumentsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeDocuments() = object {
            val fry = fry()
            val workspace = workspace(owner = fry, name = "Planet Express")
            val doc1 = document(workspace = workspace, name = "Slurm receipt", createdAt = MOCK_TIME.plusSeconds(100))
            val doc2 = document(workspace = workspace, name = "Robot oil invoice", createdAt = MOCK_TIME.plusSeconds(200))
            val doc3 = document(workspace = workspace, name = "Spaceship fuel bill", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeDocuments() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 2) {
                        edges {
                            cursor
                            node { name }
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
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.doc3.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Spaceship fuel bill") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.doc2.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Robot oil invoice") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.doc3.createdAt!!))
                                put("endCursor", encodeCursor(testData.doc2.createdAt!!))
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
            val testData = preconditions { threeDocuments() }
            val afterCursor = encodeCursor(testData.doc2.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10, after = afterCursor) {
                        edges {
                            cursor
                            node { name }
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
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.doc1.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Slurm receipt") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.doc1.createdAt!!))
                                put("endCursor", encodeCursor(testData.doc1.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no documents exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            cursor
                            node { name }
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
                        put("documents", emptyDocumentsConnection(
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
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc1 = document(workspace = workspace, name = "Slurm receipt", createdAt = MOCK_TIME.plusSeconds(100))
                }
            }
            val afterCursor = encodeCursor(testData.doc1.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10, after = afterCursor) {
                        edges {
                            cursor
                            node { name }
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
                        put("documents", emptyDocumentsConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions { threeDocuments() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 3) {
                        edges {
                            node { name }
                        }
                        pageInfo {
                            hasNextPage
                            hasPreviousPage
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                documentEdge(name = "Spaceship fuel bill")
                                documentEdge(name = "Robot oil invoice")
                                documentEdge(name = "Slurm receipt")
                            }
                            put("pageInfo", buildJsonObject {
                                put("hasPreviousPage", false)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should not include documents from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc1 = document(workspace = workspace, name = "Slurm receipt", createdAt = MOCK_TIME.plusSeconds(100))
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg(), name = "Zoidberg's Clinic")
                    document(workspace = otherWorkspace, name = "Crab food receipt")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            node { name }
                        }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                documentEdge(name = "Slurm receipt")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should order by createdAt descending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                }.also {
                    document(workspace = it.workspace, name = "Spaceship fuel bill", createdAt = MOCK_TIME.plusSeconds(300))
                    document(workspace = it.workspace, name = "Slurm receipt", createdAt = MOCK_TIME.plusSeconds(100))
                    document(workspace = it.workspace, name = "Robot oil invoice", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            node { name }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                documentEdge(name = "Spaceship fuel bill")
                                documentEdge(name = "Robot oil invoice")
                                documentEdge(name = "Slurm receipt")
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Document Fields")
    inner class DocumentFields {
        @Test
        fun `should return all document fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(
                        workspace = workspace,
                        name = "Dark matter fuel receipt",
                        timeUploaded = Instant.parse("3024-06-15T10:30:00Z"),
                        sizeInBytes = 42_000L,
                        mimeType = "application/pdf",
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            node {
                                id
                                version
                                name
                                timeUploaded
                                sizeInBytes
                                storageId
                                mimeType
                                usedBy {
                                    type
                                    relatedEntityId
                                    displayName
                                }
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.doc.id!!.toInt())
                                        put("version", testData.doc.version!!)
                                        put("name", "Dark matter fuel receipt")
                                        put("timeUploaded", "3024-06-15T10:30:00Z")
                                        put("sizeInBytes", 42_000)
                                        put("storageId", testData.doc.storageId)
                                        put("mimeType", "application/pdf")
                                        putJsonArray("usedBy") {}
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should handle null sizeInBytes`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(
                        workspace = workspace,
                        name = "Quantum torpedo schematic",
                        sizeInBytes = null,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                sizeInBytes
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("name", "Quantum torpedo schematic")
                                        put("sizeInBytes", null as Int?)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Document Usages")
    inner class DocumentUsages {

        private fun executeAndVerifyUsages(
            preconditionsSpec: EntitiesFactory.(Workspace, Document) -> List<Triple<String, Long, String>>,
        ) {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Test receipt")
                    val usages = preconditionsSpec(workspace, doc)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
                                    displayName
                                }
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("name", "Test receipt")
                                        putJsonArray("usedBy") {
                                            testData.usages
                                                .forEach { (type, id, displayName) ->
                                                    add(usageJson(type, id.toInt(), displayName))
                                                }
                                        }
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return usages for document attached to an expense`() {
            executeAndVerifyUsages { workspace, doc ->
                val expense = expense(workspace = workspace, title = "Slurm can receipt", attachments = setOf(doc))
                listOf(Triple("EXPENSE", expense.id!!, "Slurm can receipt"))
            }
        }

        @Test
        fun `should return usages for document attached to an income`() {
            executeAndVerifyUsages { workspace, doc ->
                val income = income(workspace = workspace, title = "Delivery payment", attachments = setOf(doc))
                listOf(Triple("INCOME", income.id!!, "Delivery payment"))
            }
        }

        @Test
        fun `should return usages for document attached to an invoice`() {
            executeAndVerifyUsages { workspace, doc ->
                val invoice = invoice(title = "Planet Express invoice", attachments = setOf(doc))
                listOf(Triple("INVOICE", invoice.id!!, "Planet Express invoice"))
            }
        }

        @Test
        fun `should return usages for document attached to an income tax payment`() {
            executeAndVerifyUsages { workspace, doc ->
                val taxPayment = incomeTaxPayment(workspace = workspace, title = "Mars colony tax", attachments = setOf(doc))
                listOf(Triple("INCOME_TAX_PAYMENT", taxPayment.id!!, "Mars colony tax"))
            }
        }

        @Test
        fun `should return empty usedBy for unused document`() {
            executeAndVerifyUsages { _, _ -> emptyList() }
        }

        @Test
        fun `should return multiple usages for document used by different entity types`() {
            executeAndVerifyUsages { workspace, doc ->
                val expense = expense(workspace = workspace, title = "Slurm supplies", attachments = setOf(doc))
                val income = income(workspace = workspace, title = "Delivery payment", attachments = setOf(doc))
                listOf(Triple("EXPENSE", expense.id!!, "Slurm supplies"), Triple("INCOME", income.id!!, "Delivery payment"))
            }
        }

        @Test
        fun `should return multiple usages when document is attached to multiple entities of same type`() {
            executeAndVerifyUsages { workspace, doc ->
                val expense1 = expense(workspace = workspace, title = "Slurm supplies", attachments = setOf(doc))
                val expense2 = expense(workspace = workspace, title = "Robot oil", attachments = setOf(doc))
                listOf(Triple("EXPENSE", expense1.id!!, "Slurm supplies"), Triple("EXPENSE", expense2.id!!, "Robot oil"))
            }
        }

        @Test
        fun `should return correct usages for multiple documents with mixed usage patterns`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val usedDoc = document(workspace = workspace, name = "Used receipt", createdAt = MOCK_TIME.plusSeconds(100))
                    val unusedDoc = document(workspace = workspace, name = "Unused receipt", createdAt = MOCK_TIME.plusSeconds(200))
                    val expense = expense(workspace = workspace, title = "Slurm supplies", attachments = setOf(usedDoc))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
                                    displayName
                                }
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("name", "Unused receipt")
                                        putJsonArray("usedBy") {}
                                    })
                                })
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("name", "Used receipt")
                                        putJsonArray("usedBy") {
                                            add(usageJson("EXPENSE", testData.expense.id!!.toInt(), "Slurm supplies"))
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

private fun usageJson(type: String, relatedEntityId: Int, displayName: String): JsonElement = buildJsonObject {
    put("type", type)
    put("relatedEntityId", relatedEntityId)
    put("displayName", displayName)
}

private fun emptyDocumentsConnection(
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

private fun kotlinx.serialization.json.JsonArrayBuilder.documentEdge(name: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("name", name) })
    })
}
