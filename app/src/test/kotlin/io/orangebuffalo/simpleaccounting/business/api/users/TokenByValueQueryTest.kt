package io.orangebuffalo.simpleaccounting.business.api.users

import io.kotest.matchers.collections.shouldContainOnly
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("tokenByValue query")
class TokenByValueQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val expiredToken = userActivationToken(
                token = "expired-token",
                expiresAt = MOCK_TIME.minusSeconds(1),
            )
            val activeToken = userActivationToken(
                token = "active-token",
                expiresAt = MOCK_TIME.plusSeconds(1),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should allow anonymous access`() {
            client.graphql { fullTokenByValue(token = preconditions.activeToken.token) }
                .fromAnonymous()
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.TokenByValue to buildJsonObject {
                        put("token", "active-token")
                        put("expiresAt", "1999-03-28T23:01:03.042Z")
                    }
                )
        }

        @Test
        fun `should allow regular user access`() {
            client.graphql { fullTokenByValue(token = preconditions.activeToken.token) }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.TokenByValue to buildJsonObject {
                        put("token", "active-token")
                        put("expiresAt", "1999-03-28T23:01:03.042Z")
                    }
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return null for non-existing token`() {
            client.graphql { fullTokenByValue(token = "non-existing-token") }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.TokenByValue to JsonNull
                )
        }

        @Test
        fun `should return null for expired token and remove it`() {
            client.graphql { fullTokenByValue(token = preconditions.expiredToken.token) }
                .fromAnonymous()
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.TokenByValue to JsonNull
                )

            withHint("Expired token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldContainOnly(preconditions.activeToken)
            }
        }

        @Test
        fun `should return active token`() {
            client.graphql { fullTokenByValue(token = preconditions.activeToken.token) }
                .fromAnonymous()
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.TokenByValue to buildJsonObject {
                        put("token", "active-token")
                        put("expiresAt", "1999-03-28T23:01:03.042Z")
                    }
                )
        }
    }

    private fun QueryProjection.fullTokenByValue(token: String): QueryProjection =
        tokenByValue(token = token) {
            this.token
            expiresAt
        }
}
