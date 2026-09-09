package io.orangebuffalo.simpleaccounting.business.api.auth

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.Duration

class InvalidateRefreshTokenMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should revoke and clear the refresh token when called anonymously`() {
            val preconditions = preconditions {
                object {
                    val refreshToken = RefreshToken(
                        userId = fry().id!!,
                        token = "refresh-token-for-fry",
                        expirationTime = MOCK_TIME.plus(Duration.ofDays(1)),
                    ).save()
                }
            }

            client
                .graphqlMutation { invalidateRefreshTokenMutation() }
                .fromAnonymous()
                .cookie("refreshToken", preconditions.refreshToken.token)
                .execute()
                .expectStatus().isOk
                .expectHeader().value(HttpHeaders.SET_COOKIE) { cookie ->
                    cookie.shouldContain("refreshToken=")
                    cookie.shouldContain("Max-Age=0")
                    cookie.shouldContain("Path=/api")
                    cookie.lowercase().shouldContain("httponly")
                    cookie.shouldContain("SameSite=Strict")
                }
                .expectBody()
                .jsonPath("$.data.invalidateRefreshToken").isEqualTo(true)

            aggregateTemplate.findAll<RefreshToken>().shouldBeEmpty()
        }

        @Test
        fun `should clear the refresh token cookie when called as authenticated user`() {
            val preconditions = preconditions {
                object {
                    val fry = fry().withWorkspace()
                }
            }

            client
                .graphqlMutation { invalidateRefreshTokenMutation() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.MUTATION.InvalidateRefreshToken to JsonPrimitive(true)
                )
        }
    }

    private fun MutationProjection.invalidateRefreshTokenMutation(): MutationProjection =
        apply { invalidateRefreshToken }
}
