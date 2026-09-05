package io.orangebuffalo.simpleaccounting.business.api.profile

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("myOAuthProviderLinks query")
class MyOAuthProviderLinksQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val nimbus = oauthProvider(name = "Nimbus Auth")
            val momCorp = oauthProvider(name = "MomCorp ID")
            val fry = fry().also {
                userOAuthIdentity(user = it, provider = nimbus, externalId = "fry-at-nimbus")
            }
            val bender = bender()
            val fryWorkspaceAccessToken = workspaceAccessToken(
                workspace = workspace(owner = fry),
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql { myOAuthProviderLinksQuery() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.MyOAuthProviderLinks)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphql { myOAuthProviderLinksQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.MyOAuthProviderLinks)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return all providers sorted by name with the linked identity of the current user`() {
            client.graphql { myOAuthProviderLinksQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.MyOAuthProviderLinks to buildJsonArray {
                        add(buildJsonObject {
                            put("providerId", preconditions.momCorp.id!!)
                            put("providerName", "MomCorp ID")
                            put("externalId", null as String?)
                        })
                        add(buildJsonObject {
                            put("providerId", preconditions.nimbus.id!!)
                            put("providerName", "Nimbus Auth")
                            put("externalId", "fry-at-nimbus")
                        })
                    }
                )
        }

        @Test
        fun `should not expose identities of other users`() {
            client.graphql { myOAuthProviderLinksQuery() }
                .from(preconditions.bender)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.MyOAuthProviderLinks to buildJsonArray {
                        add(buildJsonObject {
                            put("providerId", preconditions.momCorp.id!!)
                            put("providerName", "MomCorp ID")
                            put("externalId", null as String?)
                        })
                        add(buildJsonObject {
                            put("providerId", preconditions.nimbus.id!!)
                            put("providerName", "Nimbus Auth")
                            put("externalId", null as String?)
                        })
                    }
                )
        }
    }

    private fun QueryProjection.myOAuthProviderLinksQuery() = myOAuthProviderLinks {
        providerId
        providerName
        externalId
    }
}
