package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.OAuthMocks
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("discoverOidcProviderConfiguration query")
class DiscoverOidcProviderConfigurationQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {
    private val providerBaseUrl = OAuthMocks.issuerUrl("oidc-discovery").toString()

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
            val workspaceToken = workspaceAccessToken(workspace = workspace(owner = fry))
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should reject anonymous requests`() {
            client.graphql { discoveryQuery() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(DgsConstants.QUERY.DiscoverOidcProviderConfiguration)
        }

        @Test
        fun `should reject regular users`() {
            client.graphql { discoveryQuery() }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(DgsConstants.QUERY.DiscoverOidcProviderConfiguration)
        }

        @Test
        fun `should reject workspace tokens`() {
            client.graphql { discoveryQuery() }
                .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
                .executeAndVerifyNotAuthorized(DgsConstants.QUERY.DiscoverOidcProviderConfiguration)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return discovered configuration`() {
            client.graphql { discoveryQuery() }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.DiscoverOidcProviderConfiguration to buildJsonObject {
                        put("authorizationUrl", "$providerBaseUrl/authorize")
                        put("tokenUrl", "$providerBaseUrl/token")
                        put("userInfoUrl", "$providerBaseUrl/userinfo")
                        put("userIdAttribute", "sub")
                        putJsonArray("scopes") { add(JsonPrimitive("openid")) }
                    }
                )
        }

        @Test
        fun `should return DISCOVERY_FAILED when configuration cannot be loaded`() {
            client.graphql {
                discoveryQuery("$providerBaseUrl?not-a-discovery-document=true")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessErrorCode(
                    errorCode = "DISCOVERY_FAILED",
                    path = DgsConstants.QUERY.DiscoverOidcProviderConfiguration,
                )
        }
    }

    private fun QueryProjection.discoveryQuery(baseUrl: String = providerBaseUrl) =
        discoverOidcProviderConfiguration(baseUrl = baseUrl) {
            authorizationUrl
            tokenUrl
            userInfoUrl
            userIdAttribute
            scopes
        }
}
