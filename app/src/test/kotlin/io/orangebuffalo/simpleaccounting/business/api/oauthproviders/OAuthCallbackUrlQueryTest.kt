package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("oauthCallbackUrl query")
class OAuthCallbackUrlQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql { oauthCallbackUrl }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.OauthCallbackUrl)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphql { oauthCallbackUrl }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.OauthCallbackUrl)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return the callback URL based on the public URL of the application`() {
            whenever(simpleAccountingProperties.publicUrl) doReturn "https://accounting.planet-express.example"

            client.graphql { oauthCallbackUrl }
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.OauthCallbackUrl to
                            JsonPrimitive("https://accounting.planet-express.example/oauth-identity-callback")
                )
        }
    }
}
