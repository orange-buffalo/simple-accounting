package io.orangebuffalo.simpleaccounting.business.api.auth

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("userAuthenticationMethods query")
class UserAuthenticationMethodsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Password authentication")
    inner class PasswordAuthentication {
        @Test
        fun `should offer password login for users without linked identities`() {
            preconditions {
                object {
                    val fry = fry()
                }
            }

            client.graphql {
                userAuthenticationMethodsQuery(userName = "Fry")
            }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.UserAuthenticationMethods to buildJsonArray {
                        add(passwordMethod())
                    }
                )
        }

        @Test
        fun `should offer password login for unknown users to avoid disclosing accounts`() {
            client.graphql {
                userAuthenticationMethodsQuery(userName = "Lrrr")
            }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.UserAuthenticationMethods to buildJsonArray {
                        add(passwordMethod())
                    }
                )
        }
    }

    @Nested
    @DisplayName("OAuth authentication")
    inner class OAuthAuthentication {
        @Test
        fun `should only offer linked providers sorted by name when identities are linked`() {
            val testData = preconditions {
                object {
                    val nimbus = oauthProvider(name = "Nimbus Auth")
                    val momCorp = oauthProvider(name = "MomCorp ID")
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                        userOAuthIdentity(user = it, provider = momCorp, externalId = "fry-at-momcorp")
                    }
                }.also {
                    oauthProvider(name = "Globetrotters SSO")
                }
            }

            client.graphql {
                userAuthenticationMethodsQuery(userName = "Fry")
            }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.UserAuthenticationMethods to buildJsonArray {
                        add(buildJsonObject {
                            put("type", "OAUTH")
                            put("providerId", testData.momCorp.id!!)
                            put("providerName", "MomCorp ID")
                        })
                        add(buildJsonObject {
                            put("type", "OAUTH")
                            put("providerId", testData.nimbus.id!!)
                            put("providerName", "Nimbus Auth")
                        })
                    }
                )
        }

        @Test
        fun `should not disclose the activation state of a linked account`() {
            preconditions {
                object {
                    val nimbus = oauthProvider(name = "Nimbus Auth")
                    val scruffy = platformUser(userName = "Scruffy", activated = false).also {
                        userOAuthIdentity(user = it, provider = nimbus, externalId = "scruffy-at-nimbus")
                    }
                }
            }

            client.graphql {
                userAuthenticationMethodsQuery(userName = "Scruffy")
            }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.UserAuthenticationMethods to buildJsonArray {
                        add(passwordMethod())
                    }
                )
        }

        @Test
        fun `should not offer password login for users of another account with linked identities`() {
            preconditions {
                object {
                    val nimbus = oauthProvider(name = "Nimbus Auth")
                    val fry = fry().also {
                        userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
                    }
                    val bender = bender()
                }
            }

            client.graphql {
                userAuthenticationMethodsQuery(userName = "Bender")
            }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.UserAuthenticationMethods to buildJsonArray {
                        add(passwordMethod())
                    }
                )
        }
    }

    private fun passwordMethod() = buildJsonObject {
        put("type", "PASSWORD")
        put("providerId", null as String?)
        put("providerName", null as String?)
    }

    private fun QueryProjection.userAuthenticationMethodsQuery(
        userName: String,
    ) = userAuthenticationMethods(userName = userName) {
        type
        providerId
        providerName
    }
}
