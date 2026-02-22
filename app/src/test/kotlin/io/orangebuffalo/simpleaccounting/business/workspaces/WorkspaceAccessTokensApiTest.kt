package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockZoidbergUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME_VALUE
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

val ANOTHER_MOCK_TIME: Instant =
    ZonedDateTime.of(1999, 6, 28, 18, 1, 2, 53000000, ZoneId.of("America/New_York")).toInstant()
const val ANOTHER_MOCK_TIME_VALUE = "1999-06-28T22:01:02.053Z"

@DisplayName("Workspace Access Tokens API ")
class WorkspaceAccessTokensApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/123/workspace-access-tokens")
            .verifyUnauthorized()
    }

    @Test
    @WithMockZoidbergUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/workspace-access-tokens")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${preconditions.fryWorkspace.id} is not found")
            }
    }

    @Test
    @WithMockFryUser
    fun `should return tokens of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/workspace-access-tokens")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("id", preconditions.secondFryToken.id)
                        put("version", 0)
                        put("validTill", ANOTHER_MOCK_TIME_VALUE)
                        put("revoked", true)
                        put("token", "test-token-two")
                    }
                    addJsonObject {
                        put("id", preconditions.firstFryToken.id)
                        put("version", 0)
                        put("validTill", MOCK_TIME_VALUE)
                        put("revoked", false)
                        put("token", "test-token-one")
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should filter by workspace on GET`() {
        client.get()
            .uri("/api/workspaces/${preconditions.emptyFryWorkspace.id}/workspace-access-tokens")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 0)
                putJsonArray("data") {}
            }
    }

    @Test
    fun `should allow POST access only for logged in users`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/workspace-access-tokens")
            .verifyUnauthorized()
    }

    @Test
    @WithMockZoidbergUser
    fun `should return 404 on POST if workspace belongs to another user`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/workspace-access-tokens")
            .sendJson(
                """{
                    "validTill": "$MOCK_TIME_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${preconditions.fryWorkspace.id} is not found")
            }
    }

    @Test
    @WithMockFryUser
    fun `should create a new access token`() {
        whenever(tokenGenerator.generateToken()) doReturn ("new-token")

        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/workspace-access-tokens")
            .sendJson(
                """{
                    "validTill": "$ANOTHER_MOCK_TIME_VALUE"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    validTill: "$ANOTHER_MOCK_TIME_VALUE",
                    revoked: false,
                    token: "new-token"
                }"""
            )
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val zoidberg = zoidberg()
            val fryWorkspace = workspace(owner = fry)
            val emptyFryWorkspace = workspace(owner = fry)
            val firstFryToken = workspaceAccessToken(
                workspace = fryWorkspace,
                token = "test-token-one",
                revoked = false,
                validTill = MOCK_TIME
            )
            val secondFryToken = workspaceAccessToken(
                workspace = fryWorkspace,
                token = "test-token-two",
                revoked = true,
                validTill = ANOTHER_MOCK_TIME
            )

        }
    }
}
