package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
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

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry, name = "Planet Express")
            val zoidberg = zoidberg().withWorkspace()
            val workspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return error when accessed anonymously`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node { name }
                        }
                    }
                }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = "workspace",
                )
        }

        @Test
        fun `should allow access with workspace token`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node { name }
                        }
                        totalCount
                    }
                }
            }
                .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("documents", documentsConnection(totalCount = 0))
                    }
                )
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeDocuments() = object {
            val fry = fry()
            val workspace = workspace(owner = fry, name = "Planet Express")
            val doc1 = document(workspace = workspace, name = "Slurm receipt").also {
                it.createdAt = MOCK_TIME.plusSeconds(100)
                it.save()
            }
            val doc2 = document(workspace = workspace, name = "Robot oil invoice").also {
                it.createdAt = MOCK_TIME.plusSeconds(200)
                it.save()
            }
            val doc3 = document(workspace = workspace, name = "Spaceship fuel bill").also {
                it.createdAt = MOCK_TIME.plusSeconds(300)
                it.save()
            }
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeDocuments() }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
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
                                    put("cursor", encodeCursor(testData.doc1.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Slurm receipt") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.doc2.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Robot oil invoice") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.doc1.createdAt!!))
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
                workspace(id = testData.workspace.id!!.toInt()) {
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
                                    put("cursor", encodeCursor(testData.doc3.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Spaceship fuel bill") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.doc3.createdAt!!))
                                put("endCursor", encodeCursor(testData.doc3.createdAt!!))
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
                workspace(id = testData.workspace.id!!.toInt()) {
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
                        put("documents", documentsConnection(
                            totalCount = 0,
                            hasPreviousPage = false,
                            hasNextPage = false,
                            includePageInfo = true,
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
                    val doc1 = document(workspace = workspace, name = "Slurm receipt").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                }
            }
            val afterCursor = encodeCursor(testData.doc1.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
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
                        put("documents", documentsConnection(
                            totalCount = 1,
                            hasPreviousPage = true,
                            hasNextPage = false,
                            includePageInfo = true,
                        ))
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions { threeDocuments() }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
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
                        put("documents", documentsConnection(totalCount = 3, includePageInfo = true) {
                            documentEdge(name = "Slurm receipt")
                            documentEdge(name = "Robot oil invoice")
                            documentEdge(name = "Spaceship fuel bill")
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
                    val doc1 = document(workspace = workspace, name = "Slurm receipt").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg(), name = "Zoidberg's Clinic")
                    document(workspace = otherWorkspace, name = "Crab food receipt")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
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
                        put("documents", documentsConnection(totalCount = 1) {
                            documentEdge(name = "Slurm receipt")
                        })
                    }
                )
        }

        @Test
        fun `should order by createdAt ascending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                }.also {
                    document(workspace = it.workspace, name = "Spaceship fuel bill").also { doc ->
                        doc.createdAt = MOCK_TIME.plusSeconds(300)
                        doc.save()
                    }
                    document(workspace = it.workspace, name = "Slurm receipt").also { doc ->
                        doc.createdAt = MOCK_TIME.plusSeconds(100)
                        doc.save()
                    }
                    document(workspace = it.workspace, name = "Robot oil invoice").also { doc ->
                        doc.createdAt = MOCK_TIME.plusSeconds(200)
                        doc.save()
                    }
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
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
                        put("documents", documentsConnection {
                            documentEdge(name = "Slurm receipt")
                            documentEdge(name = "Robot oil invoice")
                            documentEdge(name = "Spaceship fuel bill")
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
                workspace(id = testData.workspace.id!!.toInt()) {
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
                workspace(id = testData.workspace.id!!.toInt()) {
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

        @Test
        fun `should return usages for document attached to an expense`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Slurm receipt")
                    val expense = expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(doc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Slurm receipt")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "EXPENSE")
                                                put("relatedEntityId", testData.expense.id!!.toInt())
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
        fun `should return usages for document attached to an income`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Delivery payment proof")
                    val income = income(
                        workspace = workspace,
                        title = "Delivery to Omicron Persei 8",
                        attachments = setOf(doc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Delivery payment proof")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "INCOME")
                                                put("relatedEntityId", testData.income.id!!.toInt())
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
        fun `should return usages for document attached to an invoice`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val customer = customer(workspace = workspace, name = "MomCorp")
                    val doc = document(workspace = workspace, name = "Delivery invoice scan")
                    val invoice = invoice(
                        customer = customer,
                        title = "Interplanetary delivery",
                        attachments = setOf(doc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Delivery invoice scan")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "INVOICE")
                                                put("relatedEntityId", testData.invoice.id!!.toInt())
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
        fun `should return usages for document attached to an income tax payment`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Tax filing confirmation")
                    val taxPayment = incomeTaxPayment(
                        workspace = workspace,
                        title = "Earth tax filing 3025",
                        attachments = setOf(doc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Tax filing confirmation")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "INCOME_TAX_PAYMENT")
                                                put("relatedEntityId", testData.taxPayment.id!!.toInt())
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
        fun `should return empty usedBy for unused document`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Orphaned receipt")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Orphaned receipt")
                                        putJsonArray("usedBy") {}
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return multiple usages for document used by different entity types`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Multi-use receipt")
                    val expense = expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(doc),
                    )
                    val income = income(
                        workspace = workspace,
                        title = "Delivery to Mars",
                        attachments = setOf(doc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Multi-use receipt")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "EXPENSE")
                                                put("relatedEntityId", testData.expense.id!!.toInt())
                                            })
                                            add(buildJsonObject {
                                                put("type", "INCOME")
                                                put("relatedEntityId", testData.income.id!!.toInt())
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
        fun `should return multiple usages when document is attached to multiple entities of same type`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val doc = document(workspace = workspace, name = "Shared receipt")
                    val expense1 = expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(doc),
                    )
                    val expense2 = expense(
                        workspace = workspace,
                        title = "Robot oil",
                        attachments = setOf(doc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Shared receipt")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "EXPENSE")
                                                put("relatedEntityId", testData.expense1.id!!.toInt())
                                            })
                                            add(buildJsonObject {
                                                put("type", "EXPENSE")
                                                put("relatedEntityId", testData.expense2.id!!.toInt())
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
        fun `should return correct usages for multiple documents with mixed usage patterns`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry, name = "Planet Express")
                    val usedDoc = document(workspace = workspace, name = "Used receipt").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                    val unusedDoc = document(workspace = workspace, name = "Unused receipt").also {
                        it.createdAt = MOCK_TIME.plusSeconds(200)
                        it.save()
                    }
                    val expense = expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(usedDoc),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    documents(first = 10) {
                        edges {
                            node {
                                name
                                usedBy {
                                    type
                                    relatedEntityId
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
                                        put("name", "Used receipt")
                                        putJsonArray("usedBy") {
                                            add(buildJsonObject {
                                                put("type", "EXPENSE")
                                                put("relatedEntityId", testData.expense.id!!.toInt())
                                            })
                                        }
                                    })
                                })
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("name", "Unused receipt")
                                        putJsonArray("usedBy") {}
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}

private fun documentsConnection(
    totalCount: Int? = null,
    hasPreviousPage: Boolean = false,
    hasNextPage: Boolean = false,
    includePageInfo: Boolean = false,
    edgesSpec: (kotlinx.serialization.json.JsonArrayBuilder.() -> Unit)? = null,
): JsonElement = buildJsonObject {
    putJsonArray("edges") {
        if (edgesSpec != null) {
            edgesSpec()
        }
    }
    if (totalCount != null) {
        put("totalCount", totalCount)
    }
    if (includePageInfo) {
        put("pageInfo", buildJsonObject {
            put("hasPreviousPage", hasPreviousPage)
            put("hasNextPage", hasNextPage)
            if (edgesSpec == null) {
                put("startCursor", null as String?)
                put("endCursor", null as String?)
            }
        })
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.documentEdge(name: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("name", name) })
    })
}
