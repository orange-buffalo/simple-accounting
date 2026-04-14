package io.orangebuffalo.simpleaccounting.business.api.users

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import io.kotest.matchers.collections.shouldContainOnly
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("tokenByUser query")
class TokenByUserQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
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
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql { tokenByUser(userId = 42) { token } }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.TokenByUser, ignoreExtraData = true)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphql { tokenByUser(userId = 42) { token } }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.TokenByUser, ignoreExtraData = true)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return null for non-existing token`() {
            client.graphql { fullTokenByUser(userId = 42) }
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.TokenByUser to JsonNull
                )
        }

        @Test
        fun `should return null for expired token and remove it`() {
            client.graphql { fullTokenByUser(userId = preconditions.expiredToken.userId) }
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.TokenByUser to JsonNull
                )

            withHint("Expired token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldContainOnly(preconditions.activeToken)
            }
        }

        @Test
        fun `should return active token`() {
            client.graphql { fullTokenByUser(userId = preconditions.activeToken.userId) }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.TokenByUser to buildJsonObject {
                        put("token", "active-token")
                        put("expiresAt", "1999-03-28T23:01:03.042Z")
                    }
                )
        }
    }

    private fun QueryProjection.fullTokenByUser(userId: Long): QueryProjection =
        tokenByUser(userId = userId) {
            token
            expiresAt
        }
}
