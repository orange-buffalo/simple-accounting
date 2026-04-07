package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
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
import java.time.LocalDate

class IncomeTaxPaymentsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threePayments() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val payment1 = incomeTaxPayment(workspace = workspace, title = "Q1 Tax", createdAt = MOCK_TIME.plusSeconds(100))
            val payment2 = incomeTaxPayment(workspace = workspace, title = "Q2 Tax", createdAt = MOCK_TIME.plusSeconds(200))
            val payment3 = incomeTaxPayment(workspace = workspace, title = "Q3 Tax", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threePayments() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 2) {
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
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.payment3.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Q3 Tax") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.payment2.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Q2 Tax") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.payment3.createdAt!!))
                                put("endCursor", encodeCursor(testData.payment2.createdAt!!))
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
            val testData = preconditions { threePayments() }
            val afterCursor = encodeCursor(testData.payment2.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10, after = afterCursor) {
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
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.payment1.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Q1 Tax") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.payment1.createdAt!!))
                                put("endCursor", encodeCursor(testData.payment1.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no income tax payments exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
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
                        put("incomeTaxPayments", emptyIncomeTaxPaymentsConnection(
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
                    val payment1 = incomeTaxPayment(workspace = workspace, title = "Q1 Tax", createdAt = MOCK_TIME.plusSeconds(100))
                }
            }
            val afterCursor = encodeCursor(testData.payment1.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10, after = afterCursor) {
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
                        put("incomeTaxPayments", emptyIncomeTaxPaymentsConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions { threePayments() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 3) {
                        edges {
                            node { title }
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
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                incomeTaxPaymentEdge(title = "Q3 Tax")
                                incomeTaxPaymentEdge(title = "Q2 Tax")
                                incomeTaxPaymentEdge(title = "Q1 Tax")
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
        fun `should not include income tax payments from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val payment1 = incomeTaxPayment(workspace = workspace, title = "Q1 Tax", createdAt = MOCK_TIME.plusSeconds(100))
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    incomeTaxPayment(workspace = otherWorkspace, title = "Zoidberg Tax")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
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
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                incomeTaxPaymentEdge(title = "Q1 Tax")
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
                    val workspace = workspace(owner = fry)
                }.also {
                    incomeTaxPayment(workspace = it.workspace, title = "Q3 Tax", createdAt = MOCK_TIME.plusSeconds(300))
                    incomeTaxPayment(workspace = it.workspace, title = "Q1 Tax", createdAt = MOCK_TIME.plusSeconds(100))
                    incomeTaxPayment(workspace = it.workspace, title = "Q2 Tax", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
                        edges {
                            node { title }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                incomeTaxPaymentEdge(title = "Q3 Tax")
                                incomeTaxPaymentEdge(title = "Q2 Tax")
                                incomeTaxPaymentEdge(title = "Q1 Tax")
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("IncomeTaxPayment Fields")
    inner class IncomeTaxPaymentFields {
        @Test
        fun `should return all income tax payment fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val payment = incomeTaxPayment(
                        workspace = workspace,
                        title = "Annual tax payment",
                        datePaid = LocalDate.of(3025, 1, 15),
                        reportingDate = LocalDate.of(3025, 1, 31),
                        amount = 500_00L,
                        notes = "Good news, everyone! Taxes paid.",
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
                        edges {
                            node {
                                id
                                title
                                datePaid
                                reportingDate
                                amount
                                notes
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.payment.id!!.toInt())
                                        put("title", "Annual tax payment")
                                        put("datePaid", "3025-01-15")
                                        put("reportingDate", "3025-01-31")
                                        put("amount", 500_00L)
                                        put("notes", "Good news, everyone! Taxes paid.")
                                    })
                                })
                            }
                        })
                    }
                )
        }

        @Test
        fun `should return null notes when not set`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val payment = incomeTaxPayment(workspace = workspace, title = "Slurm Tax", notes = null)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
                        edges {
                            node { notes }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomeTaxPayments", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("notes", null as String?)
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
                    val payment = incomeTaxPayment(workspace = workspace, title = "Slurm Tax")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomeTaxPayments", buildJsonObject {
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
                    val payment = incomeTaxPayment(workspace = workspace, title = "Slurm Tax", attachments = setOf(doc))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomeTaxPayments", buildJsonObject {
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
                    val payment = incomeTaxPayment(
                        workspace = workspace,
                        title = "Slurm Tax",
                        attachments = setOf(doc1, doc2),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    incomeTaxPayments(first = 10) {
                        edges {
                            node { attachments { id; name } }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("incomeTaxPayments", buildJsonObject {
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

private fun kotlinx.serialization.json.JsonArrayBuilder.incomeTaxPaymentEdge(title: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("title", title) })
    })
}

private fun emptyIncomeTaxPaymentsConnection(
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
