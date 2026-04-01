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

class CustomersQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeCustomers() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val customer1 = customer(workspace = workspace, name = "MomCorp").also {
                it.createdAt = MOCK_TIME.plusSeconds(100)
                it.save()
            }
            val customer2 = customer(workspace = workspace, name = "Planet Express").also {
                it.createdAt = MOCK_TIME.plusSeconds(200)
                it.save()
            }
            val customer3 = customer(workspace = workspace, name = "Slurm Inc").also {
                it.createdAt = MOCK_TIME.plusSeconds(300)
                it.save()
            }
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeCustomers() }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 2) {
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
                        put("customers", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.customer1.createdAt!!))
                                    put("node", buildJsonObject { put("name", "MomCorp") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.customer2.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Planet Express") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.customer1.createdAt!!))
                                put("endCursor", encodeCursor(testData.customer2.createdAt!!))
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
            val testData = preconditions { threeCustomers() }
            val afterCursor = encodeCursor(testData.customer2.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 10, after = afterCursor) {
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
                        put("customers", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.customer3.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Slurm Inc") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.customer3.createdAt!!))
                                put("endCursor", encodeCursor(testData.customer3.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no customers exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 10) {
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
                        put("customers", emptyCustomersConnection(
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
                    val customer1 = customer(workspace = workspace, name = "MomCorp").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                }
            }
            val afterCursor = encodeCursor(testData.customer1.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 10, after = afterCursor) {
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
                        put("customers", emptyCustomersConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions { threeCustomers() }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 3) {
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
                        put("customers", buildJsonObject {
                            putJsonArray("edges") {
                                customerEdge(name = "MomCorp")
                                customerEdge(name = "Planet Express")
                                customerEdge(name = "Slurm Inc")
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
        fun `should not include customers from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer1 = customer(workspace = workspace, name = "MomCorp").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    customer(workspace = otherWorkspace, name = "Fishy Joe's")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 10) {
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
                        put("customers", buildJsonObject {
                            putJsonArray("edges") {
                                customerEdge(name = "MomCorp")
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }

        @Test
        fun `should order by createdAt ascending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }.also {
                    customer(workspace = it.workspace, name = "Slurm Inc").also { c ->
                        c.createdAt = MOCK_TIME.plusSeconds(300)
                        c.save()
                    }
                    customer(workspace = it.workspace, name = "MomCorp").also { c ->
                        c.createdAt = MOCK_TIME.plusSeconds(100)
                        c.save()
                    }
                    customer(workspace = it.workspace, name = "Planet Express").also { c ->
                        c.createdAt = MOCK_TIME.plusSeconds(200)
                        c.save()
                    }
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 10) {
                        edges {
                            node { name }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("customers", buildJsonObject {
                            putJsonArray("edges") {
                                customerEdge(name = "MomCorp")
                                customerEdge(name = "Planet Express")
                                customerEdge(name = "Slurm Inc")
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Customer Fields")
    inner class CustomerFields {
        @Test
        fun `should return all customer fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val customer = customer(workspace = workspace, name = "MomCorp")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!.toInt()) {
                    customers(first = 10) {
                        edges {
                            node {
                                id
                                name
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("customers", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.customer.id!!.toInt())
                                        put("name", "MomCorp")
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.customerEdge(name: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("name", name) })
    })
}

private fun emptyCustomersConnection(
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
