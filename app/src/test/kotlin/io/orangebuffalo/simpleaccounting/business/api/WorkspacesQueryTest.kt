package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WorkspacesQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry, name = "Planet Express").also {
                val delivery = category(name = "Delivery", workspace = it)
                val maintenance = category(name = "Robot maintenance", workspace = it)
                expense(title = "Slurm supplies", workspace = it, category = delivery)
                expense(title = "Robot oil", workspace = it, category = maintenance)
                expense(title = "Spaceship parts", workspace = it, category = null)
            }
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
                workspaces(first = 10) {
                    edges {
                        node { name }
                    }
                }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.Workspaces,
                )
        }

        @Test
        fun `should prohibit access with workspace token`() {
            client.graphql {
                workspaces(first = 10) {
                    edges {
                        node { name }
                    }
                }
            }
                .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.Workspaces,
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return only current user workspaces`() {
            client.graphql {
                workspaces(first = 10) {
                    edges {
                        node { name }
                    }
                    totalCount
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject {
                                    put("name", "Planet Express")
                                })
                            })
                        }
                        put("totalCount", 1)
                    }
                )
        }

        @Test
        fun `should return workspaces with categories and expenses with categories`() {
            client.graphql {
                workspaces(first = 10) {
                    edges {
                        node {
                            name
                            categories { name }
                            expenses {
                                title
                                category { name }
                            }
                        }
                    }
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject {
                                    put("name", "Planet Express")
                                    putJsonArray("categories") {
                                        add(buildJsonObject { put("name", "Delivery") })
                                        add(buildJsonObject { put("name", "Robot maintenance") })
                                    }
                                    putJsonArray("expenses") {
                                        add(buildJsonObject {
                                            put("title", "Slurm supplies")
                                            put("category", buildJsonObject { put("name", "Delivery") })
                                        })
                                        add(buildJsonObject {
                                            put("title", "Robot oil")
                                            put("category", buildJsonObject { put("name", "Robot maintenance") })
                                        })
                                        add(buildJsonObject {
                                            put("title", "Spaceship parts")
                                            put("category", null as String?)
                                        })
                                    }
                                })
                            })
                        }
                    }
                )
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {
        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val ws1 = workspace(owner = fry, name = "Delivery to Luna Park").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                    val ws2 = workspace(owner = fry, name = "Delivery to Omicron Persei 8").also {
                        it.createdAt = MOCK_TIME.plusSeconds(200)
                        it.save()
                    }
                    val ws3 = workspace(owner = fry, name = "Delivery to Mars").also {
                        it.createdAt = MOCK_TIME.plusSeconds(300)
                        it.save()
                    }
                }
            }
            client.graphql {
                workspaces(first = 2) {
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
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.ws1.createdAt!!, testData.ws1.id!!))
                                put("node", buildJsonObject { put("name", "Delivery to Luna Park") })
                            })
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.ws2.createdAt!!, testData.ws2.id!!))
                                put("node", buildJsonObject { put("name", "Delivery to Omicron Persei 8") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.ws1.createdAt!!, testData.ws1.id!!))
                            put("endCursor", encodeCursor(testData.ws2.createdAt!!, testData.ws2.id!!))
                            put("hasPreviousPage", false)
                            put("hasNextPage", true)
                        })
                        put("totalCount", 3)
                    }
                )
        }

        @Test
        fun `should return second page using after cursor`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val ws1 = workspace(owner = fry, name = "Delivery to Luna Park").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                    val ws2 = workspace(owner = fry, name = "Delivery to Omicron Persei 8").also {
                        it.createdAt = MOCK_TIME.plusSeconds(200)
                        it.save()
                    }
                    val ws3 = workspace(owner = fry, name = "Delivery to Mars").also {
                        it.createdAt = MOCK_TIME.plusSeconds(300)
                        it.save()
                    }
                }
            }
            val afterCursor = encodeCursor(testData.ws2.createdAt!!, testData.ws2.id!!)
            client.graphql {
                workspaces(first = 10, after = afterCursor) {
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
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.ws3.createdAt!!, testData.ws3.id!!))
                                put("node", buildJsonObject { put("name", "Delivery to Mars") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.ws3.createdAt!!, testData.ws3.id!!))
                            put("endCursor", encodeCursor(testData.ws3.createdAt!!, testData.ws3.id!!))
                            put("hasPreviousPage", true)
                            put("hasNextPage", false)
                        })
                        put("totalCount", 3)
                    }
                )
        }

        @Test
        fun `should return empty connection when no workspaces exist`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                }
            }
            client.graphql {
                workspaces(first = 10) {
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
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {}
                        put("pageInfo", buildJsonObject {
                            put("startCursor", null as String?)
                            put("endCursor", null as String?)
                            put("hasPreviousPage", false)
                            put("hasNextPage", false)
                        })
                        put("totalCount", 0)
                    }
                )
        }

        @Test
        fun `should return empty page when cursor is past all items`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val ws1 = workspace(owner = fry, name = "Delivery to Luna Park").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                }
            }
            val afterCursor = encodeCursor(testData.ws1.createdAt!!, testData.ws1.id!!)
            client.graphql {
                workspaces(first = 10, after = afterCursor) {
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
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {}
                        put("pageInfo", buildJsonObject {
                            put("startCursor", null as String?)
                            put("endCursor", null as String?)
                            put("hasPreviousPage", true)
                            put("hasNextPage", false)
                        })
                        put("totalCount", 1)
                    }
                )
        }

        @Test
        fun `should return all items when first equals total count`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val ws1 = workspace(owner = fry, name = "Delivery to Luna Park").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                    val ws2 = workspace(owner = fry, name = "Delivery to Omicron Persei 8").also {
                        it.createdAt = MOCK_TIME.plusSeconds(200)
                        it.save()
                    }
                }
            }
            client.graphql {
                workspaces(first = 2) {
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
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Delivery to Luna Park") })
                            })
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Delivery to Omicron Persei 8") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("hasNextPage", false)
                            put("hasPreviousPage", false)
                        })
                        put("totalCount", 2)
                    }
                )
        }

        @Test
        fun `should not include other users workspaces in pagination`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val ws1 = workspace(owner = fry, name = "Delivery to Luna Park").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                    val zoidberg = zoidberg()
                    val zoidbergWs = workspace(owner = zoidberg, name = "Zoidberg clinic").also {
                        it.createdAt = MOCK_TIME.plusSeconds(150)
                        it.save()
                    }
                }
            }
            client.graphql {
                workspaces(first = 10) {
                    edges {
                        node { name }
                    }
                    totalCount
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Delivery to Luna Park") })
                            })
                        }
                        put("totalCount", 1)
                    }
                )
        }

        @Test
        fun `should reject first greater than 500`() {
            client.graphql {
                workspaces(first = 501) {
                    edges {
                        node { name }
                    }
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "first",
                    error = "MaxConstraintViolated",
                    message = "must be less than or equal to 500",
                    path = DgsConstants.QUERY.Workspaces,
                    params = mapOf("value" to "500"),
                )
        }

        @Test
        fun `should reject first less than 1`() {
            client.graphql {
                workspaces(first = 0) {
                    edges {
                        node { name }
                    }
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "first",
                    error = "MinConstraintViolated",
                    message = "must be greater than or equal to 1",
                    path = DgsConstants.QUERY.Workspaces,
                    params = mapOf("value" to "1"),
                )
        }

        @Test
        fun `should order by createdAt ascending`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val ws1 = workspace(owner = fry, name = "Delivery to Mars").also {
                        it.createdAt = MOCK_TIME.plusSeconds(300)
                        it.save()
                    }
                    val ws2 = workspace(owner = fry, name = "Delivery to Luna Park").also {
                        it.createdAt = MOCK_TIME.plusSeconds(100)
                        it.save()
                    }
                    val ws3 = workspace(owner = fry, name = "Delivery to Omicron Persei 8").also {
                        it.createdAt = MOCK_TIME.plusSeconds(200)
                        it.save()
                    }
                }
            }
            client.graphql {
                workspaces(first = 10) {
                    edges {
                        node { name }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspaces" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Delivery to Luna Park") })
                            })
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Delivery to Omicron Persei 8") })
                            })
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Delivery to Mars") })
                            })
                        }
                    }
                )
        }
    }
}
