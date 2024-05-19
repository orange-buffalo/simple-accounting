package io.orangebuffalo.simpleaccounting.web.api

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockZoidbergUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME_VALUE
import io.orangebuffalo.simpleaccounting.services.integration.TokenGenerator
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

val ANOTHER_MOCK_TIME: Instant =
    ZonedDateTime.of(1999, 6, 28, 18, 1, 2, 53000000, ZoneId.of("America/New_York")).toInstant()
const val ANOTHER_MOCK_TIME_VALUE = "1999-06-28T22:01:02.053Z"

@SimpleAccountingIntegrationTest
@DisplayName("Workspace Access Tokens API ")
class WorkspaceAccessTokensApiControllerIT(
    @Autowired private val client: WebTestClient,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @MockBean
    lateinit var tokenGenerator: TokenGenerator

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/123/workspace-access-tokens")
            .verifyUnauthorized()
    }

    @Test
    @WithMockZoidbergUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/workspace-access-tokens")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${testData.fryWorkspace.id} is not found")
            }
    }

    @Test
    @WithMockFryUser
    fun `should return tokens of current user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/workspace-access-tokens")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            id: ${testData.firstFryToken.id},
                            version: 0,
                            validTill: "$MOCK_TIME_VALUE",
                            revoked: false,
                            token: "test-token-one"
                        }"""
                    ),

                    json(
                        """{
                            id: ${testData.secondFryToken.id},
                            version: 0,
                            validTill: "$ANOTHER_MOCK_TIME_VALUE",
                            revoked: true,
                            token: "test-token-two"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should filter by workspace on GET`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.emptyFryWorkspace.id}/workspace-access-tokens")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("0")
                inPath("$.data").isArray.isEmpty()
            }
    }

    @Test
    fun `should allow POST access only for logged in users`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/workspace-access-tokens")
            .verifyUnauthorized()
    }

    @Test
    @WithMockZoidbergUser
    fun `should return 404 on POST if workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/workspace-access-tokens")
            .sendJson(
                """{
                    "validTill": "$MOCK_TIME_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${testData.fryWorkspace.id} is not found")
            }
    }

    @Test
    @WithMockFryUser
    fun `should create a new access token`() {
        val testData = setupPreconditions()
        whenever(tokenGenerator.generateToken()) doReturn ("new-token")

        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/workspace-access-tokens")
            .sendJson(
                """{
                    "validTill": "$ANOTHER_MOCK_TIME_VALUE"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                        id: "#{json-unit.any-number}",
                        version: 0,
                        validTill: "$ANOTHER_MOCK_TIME_VALUE",
                        revoked: false,
                        token: "new-token"
                    }"""
                    )
                )
            }
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
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
