package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class StandaloneDocumentsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeStandaloneDocuments() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val document1 = document(workspace = workspace, name = "Delivery receipt")
            val document2 = document(workspace = workspace, name = "Robot oil receipt")
            val document3 = document(workspace = workspace, name = "Slurm receipt")
            val standaloneDocument1 = standaloneDocument(
                document = document1,
                title = "Delivery to Mars",
                createdAt = MOCK_TIME.plusSeconds(100),
            )
            val standaloneDocument2 = standaloneDocument(
                document = document2,
                title = "Robot maintenance",
                createdAt = MOCK_TIME.plusSeconds(200),
            )
            val standaloneDocument3 = standaloneDocument(
                document = document3,
                title = "Slurm supplies",
                createdAt = MOCK_TIME.plusSeconds(300),
            )
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeStandaloneDocuments() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    standaloneDocuments(first = 2) {
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
                        put("standaloneDocuments", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.standaloneDocument3.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Slurm supplies") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.standaloneDocument2.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Robot maintenance") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.standaloneDocument3.createdAt!!))
                                put("endCursor", encodeCursor(testData.standaloneDocument2.createdAt!!))
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
            val testData = preconditions { threeStandaloneDocuments() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    standaloneDocuments(first = 10, after = encodeCursor(testData.standaloneDocument2.createdAt!!)) {
                        edges {
                            node { title }
                        }
                        pageInfo {
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
                        put("standaloneDocuments", buildJsonObject {
                            putJsonArray("edges") {
                                standaloneDocumentEdge(title = "Delivery to Mars")
                            }
                            put("pageInfo", buildJsonObject {
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should not include standalone documents from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val document = document(workspace = workspace)
                    val standaloneDocument = standaloneDocument(
                        document = document,
                        title = "Delivery to Mars",
                    )
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    standaloneDocument(
                        document = document(workspace = otherWorkspace),
                        title = "Doctor supplies",
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    standaloneDocuments(first = 10) {
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
                        put("standaloneDocuments", buildJsonObject {
                            putJsonArray("edges") {
                                standaloneDocumentEdge(title = "Delivery to Mars")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should return all standalone document fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val document = document(workspace = workspace, name = "Slurm receipt")
                    val standaloneDocument = standaloneDocument(
                        document = document,
                        title = "Slurm supplies",
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    standaloneDocuments(first = 10) {
                        edges {
                            node {
                                id
                                title
                                documentId
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("standaloneDocuments", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.standaloneDocument.id!!)
                                        put("title", "Slurm supplies")
                                        put("documentId", testData.document.id!!)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.standaloneDocumentEdge(title: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("title", title) })
    })
}
