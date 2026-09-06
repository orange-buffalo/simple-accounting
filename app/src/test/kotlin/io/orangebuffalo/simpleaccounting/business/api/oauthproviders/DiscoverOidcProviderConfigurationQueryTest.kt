package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OidcProviderConfiguration
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OidcProviderDiscoveryException
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OidcProviderDiscoveryService
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayName("discoverOidcProviderConfiguration query")
class DiscoverOidcProviderConfigurationQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var discoveryService: OidcProviderDiscoveryService

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
            whenever(discoveryService.discover("http://provider.example")) doReturn OidcProviderConfiguration(
                authorizationUrl = "http://provider.example/authorize",
                tokenUrl = "http://provider.example/token",
                userInfoUrl = "http://provider.example/userinfo",
                userIdAttribute = "sub",
                scopes = listOf("openid"),
            )

            client.graphql { discoveryQuery() }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.DiscoverOidcProviderConfiguration to buildJsonObject {
                        put("authorizationUrl", "http://provider.example/authorize")
                        put("tokenUrl", "http://provider.example/token")
                        put("userInfoUrl", "http://provider.example/userinfo")
                        put("userIdAttribute", "sub")
                        putJsonArray("scopes") { add(JsonPrimitive("openid")) }
                    }
                )
        }

        @Test
        fun `should return DISCOVERY_FAILED when configuration cannot be loaded`() {
            whenever(discoveryService.discover("http://provider.example"))
                .thenThrow(OidcProviderDiscoveryException())

            client.graphql { discoveryQuery() }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessErrorCode(
                    errorCode = "DISCOVERY_FAILED",
                    path = DgsConstants.QUERY.DiscoverOidcProviderConfiguration,
                )
        }
    }

    private fun QueryProjection.discoveryQuery() =
        discoverOidcProviderConfiguration(baseUrl = "http://provider.example") {
            authorizationUrl
            tokenUrl
            userInfoUrl
            userIdAttribute
            scopes
        }
}
