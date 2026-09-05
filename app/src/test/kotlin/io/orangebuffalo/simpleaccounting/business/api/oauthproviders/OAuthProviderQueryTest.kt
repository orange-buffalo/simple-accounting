package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("oauthProvider query")
class OAuthProviderQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
            val nimbus = oauthProvider(
                name = "Nimbus Auth",
                clientId = "nimbus-client-id",
                clientSecret = "nimbus-client-secret",
                authorizationUrl = "https://nimbus.example/authorize",
                tokenUrl = "https://nimbus.example/token",
                userInfoUrl = "https://nimbus.example/userinfo",
                userIdAttribute = "email",
                scopes = setOf("openid", "email"),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql {
                oauthProvider(id = preconditions.nimbus.id!!) { id }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.OauthProvider)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphql {
                oauthProvider(id = preconditions.nimbus.id!!) { id }
            }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.OauthProvider)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return the provider without exposing the client secret`() {
            client.graphql {
                oauthProvider(id = preconditions.nimbus.id!!) {
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
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.OauthProvider to buildJsonObject {
                        put("id", preconditions.nimbus.id!!)
                        put("version", 0)
                        put("name", "Nimbus Auth")
                        put("clientId", "nimbus-client-id")
                        put("authorizationUrl", "https://nimbus.example/authorize")
                        put("tokenUrl", "https://nimbus.example/token")
                        put("userInfoUrl", "https://nimbus.example/userinfo")
                        put("userIdAttribute", "email")
                        putJsonArray("scopes") {
                            add(JsonPrimitive("email"))
                            add(JsonPrimitive("openid"))
                        }
                    }
                )
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error for unknown provider`() {
            client.graphql {
                oauthProvider(id = "unknown-id") { id }
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.OauthProvider)
        }
    }
}
