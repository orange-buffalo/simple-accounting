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

class CategoriesQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeCategories() = object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category1 = category(workspace = workspace, name = "Delivery", createdAt = MOCK_TIME.plusSeconds(100))
            val category2 = category(workspace = workspace, name = "Robot maintenance", createdAt = MOCK_TIME.plusSeconds(200))
            val category3 = category(workspace = workspace, name = "Spaceship fuel", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeCategories() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 2) {
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
                        put("categories", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.category3.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Spaceship fuel") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.category2.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Robot maintenance") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.category3.createdAt!!))
                                put("endCursor", encodeCursor(testData.category2.createdAt!!))
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
            val testData = preconditions { threeCategories() }
            val afterCursor = encodeCursor(testData.category2.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 10, after = afterCursor) {
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
                        put("categories", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.category1.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Delivery") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.category1.createdAt!!))
                                put("endCursor", encodeCursor(testData.category1.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should return empty connection when no categories exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 10) {
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
                        put("categories", emptyCategoriesConnection(
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
                    val category1 = category(workspace = workspace, name = "Delivery", createdAt = MOCK_TIME.plusSeconds(100))
                }
            }
            val afterCursor = encodeCursor(testData.category1.createdAt!!)
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 10, after = afterCursor) {
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
                        put("categories", emptyCategoriesConnection(
                            hasPreviousPage = true,
                            hasNextPage = false,
                            totalCount = 1,
                        ))
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions { threeCategories() }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 3) {
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
                        put("categories", buildJsonObject {
                            putJsonArray("edges") {
                                categoryEdge(name = "Spaceship fuel")
                                categoryEdge(name = "Robot maintenance")
                                categoryEdge(name = "Delivery")
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
        fun `should not include categories from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val category1 = category(workspace = workspace, name = "Delivery", createdAt = MOCK_TIME.plusSeconds(100))
                }.also {
                    val otherWorkspace = workspace(owner = zoidberg())
                    category(workspace = otherWorkspace, name = "Planet Express expenses")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 10) {
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
                        put("categories", buildJsonObject {
                            putJsonArray("edges") {
                                categoryEdge(name = "Delivery")
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
                    category(workspace = it.workspace, name = "Spaceship fuel", createdAt = MOCK_TIME.plusSeconds(300))
                    category(workspace = it.workspace, name = "Delivery", createdAt = MOCK_TIME.plusSeconds(100))
                    category(workspace = it.workspace, name = "Robot maintenance", createdAt = MOCK_TIME.plusSeconds(200))
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 10) {
                        edges {
                            node { name }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("categories", buildJsonObject {
                            putJsonArray("edges") {
                                categoryEdge(name = "Spaceship fuel")
                                categoryEdge(name = "Robot maintenance")
                                categoryEdge(name = "Delivery")
                            }
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Category Fields")
    inner class CategoryFields {
        @Test
        fun `should return all category fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val category = category(
                        workspace = workspace,
                        name = "Delivery",
                        description = "Interplanetary delivery expenses",
                        income = false,
                        expense = true,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    categories(first = 10) {
                        edges {
                            node {
                                id
                                name
                                description
                                income
                                expense
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("categories", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.category.id!!.toInt())
                                        put("name", "Delivery")
                                        put("description", "Interplanetary delivery expenses")
                                        put("income", false)
                                        put("expense", true)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.categoryEdge(name: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("name", name) })
    })
}

private fun emptyCategoriesConnection(
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
