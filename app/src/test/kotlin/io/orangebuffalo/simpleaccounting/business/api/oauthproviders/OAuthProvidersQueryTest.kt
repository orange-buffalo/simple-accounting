package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("oauthProviders query")
class OAuthProvidersQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql {
                oauthProviders(first = 10) { totalCount }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.OauthProviders)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                }
            }
            client.graphql {
                oauthProviders(first = 10) { totalCount }
            }
                .from(testData.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.OauthProviders)
        }

        @Test
        fun `should allow access for admin user`() {
            val testData = preconditions {
                object {
                    val farnsworth = farnsworth()
                }
            }
            client.graphql {
                oauthProviders(first = 10) { totalCount }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "oauthProviders" to buildJsonObject {
                        put("totalCount", 0)
                    }
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return all providers sorted by createdAt descending`() {
            val testData = preconditions {
                object {
                    val farnsworth = farnsworth()
                    val nimbus = oauthProvider(
                        name = "Nimbus Auth",
                        clientId = "nimbus-client-id",
                        clientSecret = "nimbus-client-secret",
                        authorizationUrl = "https://nimbus.example/authorize",
                        tokenUrl = "https://nimbus.example/token",
                        userInfoUrl = "https://nimbus.example/userinfo",
                        userIdAttribute = "sub",
                        scopes = setOf("openid", "email"),
                        createdAt = MOCK_TIME,
                    )
                    val momCorp = oauthProvider(
                        name = "MomCorp ID",
                        scopes = setOf(),
                        createdAt = MOCK_TIME.plusSeconds(100),
                    )
                }
            }
            client.graphql {
                oauthProviders(first = 10) {
                    edges {
                        node {
                            id
                            version
                            name
                            clientId
                            authorizationUrl
                            tokenUrl
                            userInfoUrl
                            userIdAttribute
                            scopes
                        }
                    }
                    totalCount
                }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "oauthProviders" to buildJsonObject {
                        put("totalCount", 2)
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject {
                                    put("id", testData.momCorp.id!!)
                                    put("version", 0)
                                    put("name", "MomCorp ID")
                                    put("clientId", "test-client-id")
                                    put("authorizationUrl", "https://nimbus.example/authorize")
                                    put("tokenUrl", "https://nimbus.example/token")
                                    put("userInfoUrl", "https://nimbus.example/userinfo")
                                    put("userIdAttribute", "sub")
                                    putJsonArray("scopes") { }
                                })
                            })
                            add(buildJsonObject {
                                put("node", buildJsonObject {
                                    put("id", testData.nimbus.id!!)
                                    put("version", 0)
                                    put("name", "Nimbus Auth")
                                    put("clientId", "nimbus-client-id")
                                    put("authorizationUrl", "https://nimbus.example/authorize")
                                    put("tokenUrl", "https://nimbus.example/token")
                                    put("userInfoUrl", "https://nimbus.example/userinfo")
                                    put("userIdAttribute", "sub")
                                    putJsonArray("scopes") {
                                        add(JsonPrimitive("email"))
                                        add(JsonPrimitive("openid"))
                                    }
                                })
                            })
                        }
                    }
                )
        }

        @Test
        fun `should filter by provider name case-insensitively`() {
            val testData = preconditions {
                object {
                    val farnsworth = farnsworth()
                }.also {
                    oauthProvider(name = "Nimbus Auth")
                    oauthProvider(name = "MomCorp ID")
                }
            }
            client.graphql {
                oauthProviders(first = 10, freeSearchText = "nimbus") {
                    edges {
                        node { name }
                    }
                    totalCount
                }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "oauthProviders" to buildJsonObject {
                        put("totalCount", 1)
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Nimbus Auth") })
                            })
                        }
                    }
                )
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeProviders() = object {
            val farnsworth = farnsworth()
            val nimbus = oauthProvider(name = "Nimbus Auth", createdAt = MOCK_TIME)
            val momCorp = oauthProvider(name = "MomCorp ID", createdAt = MOCK_TIME.plusSeconds(100))
            val globetrotters = oauthProvider(name = "Globetrotters SSO", createdAt = MOCK_TIME.plusSeconds(200))
        }

        @Test
        fun `should return first page with pageInfo`() {
            val testData = preconditions { threeProviders() }
            client.graphql {
                oauthProviders(first = 2) {
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
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "oauthProviders" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.globetrotters.createdAt!!))
                                put("node", buildJsonObject { put("name", "Globetrotters SSO") })
                            })
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.momCorp.createdAt!!))
                                put("node", buildJsonObject { put("name", "MomCorp ID") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.globetrotters.createdAt!!))
                            put("endCursor", encodeCursor(testData.momCorp.createdAt!!))
                            put("hasPreviousPage", false)
                            put("hasNextPage", true)
                        })
                        put("totalCount", 3)
                    }
                )
        }

        @Test
        fun `should return second page using after cursor`() {
            val testData = preconditions { threeProviders() }
            client.graphql {
                oauthProviders(first = 10, after = encodeCursor(testData.momCorp.createdAt!!)) {
                    edges {
                        node { name }
                    }
                    pageInfo {
                        hasPreviousPage
                        hasNextPage
                    }
                    totalCount
                }
            }
                .from(testData.farnsworth)
                .executeAndVerifyResponse(
                    "oauthProviders" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("node", buildJsonObject { put("name", "Nimbus Auth") })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("hasPreviousPage", true)
                            put("hasNextPage", false)
                        })
                        put("totalCount", 3)
                    }
                )
        }
    }
}
