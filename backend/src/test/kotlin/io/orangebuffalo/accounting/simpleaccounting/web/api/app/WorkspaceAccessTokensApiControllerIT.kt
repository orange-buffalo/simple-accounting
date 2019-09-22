package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.*
import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.services.integration.TokenGenerator
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

val ANOTHER_MOCK_TIME: Instant =
    ZonedDateTime.of(1999, 6, 28, 18, 1, 2, 53000000, ZoneId.of("America/New_York")).toInstant()
const val ANOTHER_MOCK_TIME_VALUE = "1999-06-28T22:01:02.053Z"

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Workspace Access Tokens API ")
internal class WorkspaceAccessTokensApiControllerIT(
    @Autowired val client: WebTestClient
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
    fun `should return 404 on GET if workspace belongs to another user`(
        testData: WorkspaceAccessTokensApiTestData
    ) {
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
    fun `should return tokens of current user`(testData: WorkspaceAccessTokensApiTestData) {
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
    fun `should filter by workspace on GET`(testData: WorkspaceAccessTokensApiTestData) {
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
    fun `should allow POST access only for logged in users`(testData: WorkspaceAccessTokensApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/workspace-access-tokens")
            .verifyUnauthorized()
    }

    @Test
    @WithMockZoidbergUser
    fun `should return 404 on POST if workspace belongs to another user`(
        testData: WorkspaceAccessTokensApiTestData
    ) {
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
    fun `should create a new access token`(testData: WorkspaceAccessTokensApiTestData) {
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

    class WorkspaceAccessTokensApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val zoidberg = Prototypes.zoidberg()
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val emptyFryWorkspace = Prototypes.workspace(owner = fry)
        val firstFryToken = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            token = "test-token-one",
            revoked = false,
            validTill = MOCK_TIME
        )
        val secondFryToken = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            token = "test-token-two",
            revoked = true,
            validTill = ANOTHER_MOCK_TIME
        )

        override fun generateData() = listOf(
            farnsworth, fry, fryWorkspace, zoidberg,
            firstFryToken, secondFryToken, emptyFryWorkspace
        )
    }
}