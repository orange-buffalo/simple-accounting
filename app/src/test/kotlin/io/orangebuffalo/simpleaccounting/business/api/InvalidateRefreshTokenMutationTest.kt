package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders

class InvalidateRefreshTokenMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should clear the refresh token cookie when called anonymously`() {
        client
            .graphqlMutation { invalidateRefreshTokenMutation() }
            .fromAnonymous()
            .execute()
            .expectStatus().isOk
            .expectHeader().value(HttpHeaders.SET_COOKIE) { cookie ->
                assertThat(cookie).contains("refreshToken=")
                    .contains("Max-Age=0")
                    .contains("Path=/api/auth/token")
                    .contains("HttpOnly")
                    .contains("SameSite=Strict")
            }
            .expectBody()
            .jsonPath("$.data.invalidateRefreshToken").isEqualTo(true)
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
                "invalidateRefreshToken" to JsonPrimitive(true)
            )
    }

    private fun MutationProjection.invalidateRefreshTokenMutation(): MutationProjection =
        apply { invalidateRefreshToken }
}
