package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("workspace.workspaceAccessTokens query")
class WorkspaceAccessTokensQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    workspaceAccessTokens(first = 10) {
                        totalCount
                    }
                }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "workspace")
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val token = workspaceAccessToken(
                        workspace = workspace,
                        validTill = MOCK_TIME.plusSeconds(10000),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    workspaceAccessTokens(first = 10) {
                        totalCount
                    }
                }
            }
                .usingSharedWorkspaceToken(testData.token.token)
                .executeAndVerifyNotAuthorized(
                    paths = listOf("workspace", "workspaceAccessTokens"),
                    locationColumn = 5,
                    locationLine = 3,
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
                    val workspace = workspace(owner = fry)
                    val token1 = workspaceAccessToken(
                        workspace = workspace,
                        token = "token-one",
                        createdAt = MOCK_TIME.plusSeconds(100),
                    )
                    val token2 = workspaceAccessToken(
                        workspace = workspace,
                        token = "token-two",
                        createdAt = MOCK_TIME.plusSeconds(200),
                    )
                    val token3 = workspaceAccessToken(
                        workspace = workspace,
                        token = "token-three",
                        createdAt = MOCK_TIME.plusSeconds(300),
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    workspaceAccessTokens(first = 2) {
                        edges {
                            cursor
                            node { token }
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
                        put("workspaceAccessTokens", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.token3.createdAt!!))
                                    put("node", buildJsonObject { put("token", "token-three") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.token2.createdAt!!))
                                    put("node", buildJsonObject { put("token", "token-two") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.token3.createdAt!!))
                                put("endCursor", encodeCursor(testData.token2.createdAt!!))
                                put("hasPreviousPage", false)
                                put("hasNextPage", true)
                            })
                            put("totalCount", 3)
                        })
                    }
                )
        }

        @Test
        fun `should not include tokens from other workspaces`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val token = workspaceAccessToken(
                        workspace = workspace,
                        token = "fry-token",
                    )
                }.also {
                    workspaceAccessToken(workspace = workspace(owner = zoidberg()), token = "other-token")
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    workspaceAccessTokens(first = 10) {
                        edges { node { token } }
                        totalCount
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("workspaceAccessTokens", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject { put("token", "fry-token") })
                                })
                            }
                            put("totalCount", 1)
                        })
                    }
                )
        }
    }

    @Nested
    @DisplayName("Token Fields")
    inner class TokenFields {

        @Test
        fun `should return all token fields`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                    val token = workspaceAccessToken(
                        workspace = workspace,
                        token = "my-share-token",
                        validTill = MOCK_TIME.plusSeconds(9999),
                        revoked = true,
                    )
                }
            }
            client.graphql {
                workspace(id = testData.workspace.id!!) {
                    workspaceAccessTokens(first = 10) {
                        edges {
                            node {
                                id
                                version
                                token
                                validTill
                                revoked
                            }
                        }
                    }
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("workspaceAccessTokens", buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("node", buildJsonObject {
                                        put("id", testData.token.id!!.toInt())
                                        put("version", 0)
                                        put("token", "my-share-token")
                                        put("validTill", MOCK_TIME.plusSeconds(9999).toString())
                                        put("revoked", true)
                                    })
                                })
                            }
                        })
                    }
                )
        }
    }
}
