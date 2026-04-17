package io.orangebuffalo.simpleaccounting.business.api.users

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
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

@DisplayName("users query")
class UsersQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql {
                users(first = 10) { totalCount }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.Users)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                }
            }
            client.graphql {
                users(first = 10) { totalCount }
            }
                .from(testData.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.Users)
        }

        @Test
        fun `should allow access for admin user`() {
            val testData = preconditions {
                object {
                    val farnsworth = farnsworth()
                }
            }
            client.graphql {
                users(first = 10) { totalCount }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "users" to buildJsonObject {
                        put("totalCount", 1)
                    }
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return all users sorted by createdAt descending`() {
            val testData = preconditions {
                object {
                    val farnsworth = platformUser(
                        userName = "Farnsworth",
                        isAdmin = true,
                        activated = true,
                        createdAt = MOCK_TIME,
                    )
                    val fry = platformUser(
                        userName = "Fry",
                        isAdmin = false,
                        activated = true,
                        createdAt = MOCK_TIME.plusSeconds(100),
                    )
                }
            }
            client.graphql {
                users(first = 10) {
                    edges {
                        node {
                            id
                            userName
                            admin
                            activated
                        }
                    }
                    totalCount
                }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "users" to buildJsonObject {
                        put("totalCount", 2)
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject {
                                    put("id", testData.fry.id!!)
                                    put("userName", "Fry")
                                    put("admin", false)
                                    put("activated", true)
                                })
                            })
                            add(buildJsonObject {
                                put("node", buildJsonObject {
                                    put("id", testData.farnsworth.id!!)
                                    put("userName", "Farnsworth")
                                    put("admin", true)
                                    put("activated", true)
                                })
                            })
                        }
                    }
                )
        }

        @Test
        fun `should filter by username case-insensitively`() {
            val testData = preconditions {
                object {
                    val farnsworth = farnsworth()
                }.also {
                    fry()
                    zoidberg()
                }
            }
            client.graphql {
                users(first = 10, freeSearchText = "fry") {
                    edges {
                        node { userName }
                    }
                    totalCount
                }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "users" to buildJsonObject {
                        put("totalCount", 1)
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("userName", "Fry") })
                            })
                        }
                    }
                )
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.fourUsers() = object {
            val farnsworth = platformUser(userName = "Farnsworth", isAdmin = true, activated = true, createdAt = MOCK_TIME)
            val user1 = platformUser(userName = "Bender", createdAt = MOCK_TIME.plusSeconds(100))
            val user2 = platformUser(userName = "Leela", createdAt = MOCK_TIME.plusSeconds(200))
            val user3 = platformUser(userName = "Amy", createdAt = MOCK_TIME.plusSeconds(300))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { fourUsers() }
            client.graphql {
                users(first = 2) {
                    edges {
                        cursor
                        node { userName }
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
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "users" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.user3.createdAt!!))
                                put("node", buildJsonObject { put("userName", "Amy") })
                            })
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.user2.createdAt!!))
                                put("node", buildJsonObject { put("userName", "Leela") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.user3.createdAt!!))
                            put("endCursor", encodeCursor(testData.user2.createdAt!!))
                            put("hasPreviousPage", false)
                            put("hasNextPage", true)
                        })
                        put("totalCount", 4)
                    }
                )
        }

        @Test
        fun `should return second page using after cursor`() {
            val testData = preconditions { fourUsers() }
            val afterCursor = encodeCursor(testData.user2.createdAt!!)
            client.graphql {
                users(first = 10, after = afterCursor) {
                    edges {
                        cursor
                        node { userName }
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
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "users" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.user1.createdAt!!))
                                put("node", buildJsonObject { put("userName", "Bender") })
                            })
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.farnsworth.createdAt!!))
                                put("node", buildJsonObject { put("userName", "Farnsworth") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.user1.createdAt!!))
                            put("endCursor", encodeCursor(testData.farnsworth.createdAt!!))
                            put("hasPreviousPage", true)
                            put("hasNextPage", false)
                        })
                        put("totalCount", 4)
                    }
                )
        }
    }
}
