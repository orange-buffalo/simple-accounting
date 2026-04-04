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

class GeneralTaxesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeTaxes() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val tax1 = generalTax(workspace = workspace, title = "VAT", createdAt = MOCK_TIME.plusSeconds(100))
            val tax2 = generalTax(workspace = workspace, title = "Sales Tax", createdAt = MOCK_TIME.plusSeconds(200))
            val tax3 = generalTax(workspace = workspace, title = "Slurm Tax", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeTaxes() }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 2) {
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
                        put("generalTaxes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.tax3.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Slurm Tax") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.tax2.createdAt!!))
                                    put("node", buildJsonObject { put("title", "Sales Tax") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.tax3.createdAt!!))
                                put("endCursor", encodeCursor(testData.tax2.createdAt!!))
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
            val testData = preconditions { threeTaxes() }
            val afterCursor = encodeCursor(testData.tax2.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 10, after = afterCursor) {
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
                        put("generalTaxes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.tax1.createdAt!!))
                                    put("node", buildJsonObject { put("title", "VAT") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.tax1.createdAt!!))
                                put("endCursor", encodeCursor(testData.tax1.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no general taxes exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 10) {
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
                        put("generalTaxes", emptyGeneralTaxesConnection(
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
                    val tax1 = generalTax(workspace = workspace, title = "VAT", createdAt = MOCK_TIME.plusSeconds(100))
                }
            }
            val afterCursor = encodeCursor(testData.tax1.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 10, after = afterCursor) {
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
                        put("generalTaxes", emptyGeneralTaxesConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions { threeTaxes() }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 3) {
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
                        put("generalTaxes", buildJsonObject {
                            putJsonArray("edges") {
                                generalTaxEdge(title = "Slurm Tax")
                                generalTaxEdge(title = "Sales Tax")
                                generalTaxEdge(title = "VAT")
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
        fun `should not include general taxes from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val tax1 = generalTax(workspace = workspace, title = "VAT", createdAt = MOCK_TIME.plusSeconds(100))
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    generalTax(workspace = otherWorkspace, title = "Omicron Tax")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 10) {
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
                        put("generalTaxes", buildJsonObject {
                            putJsonArray("edges") {
                                generalTaxEdge(title = "VAT")
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
                    generalTax(workspace = it.workspace, title = "Slurm Tax", createdAt = MOCK_TIME.plusSeconds(300))
                    generalTax(workspace = it.workspace, title = "VAT", createdAt = MOCK_TIME.plusSeconds(100))
                    generalTax(workspace = it.workspace, title = "Sales Tax", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 10) {
                        edges {
                            node { title }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("generalTaxes", buildJsonObject {
                            putJsonArray("edges") {
                                generalTaxEdge(title = "Slurm Tax")
                                generalTaxEdge(title = "Sales Tax")
                                generalTaxEdge(title = "VAT")
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("GeneralTax Fields")
    inner class GeneralTaxFields {
        @Test
        fun `should return all general tax fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val tax = generalTax(
                        workspace = workspace,
                        title = "VAT",
                        description = "Value Added Tax for interplanetary deliveries",
                        rateInBps = 20_00,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    generalTaxes(first = 10) {
                        edges {
                            node {
                                id
                                title
                                description
                                rateInBps
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("generalTaxes", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.tax.id!!.toInt())
                                        put("title", "VAT")
                                        put("description", "Value Added Tax for interplanetary deliveries")
                                        put("rateInBps", 20_00)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.generalTaxEdge(title: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("title", title) })
    })
}

private fun emptyGeneralTaxesConnection(
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
