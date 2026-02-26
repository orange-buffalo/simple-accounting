package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.security.SaUserRoles
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

private const val LOGIN_BY_TOKEN_PATH = "/api/auth/login-by-token"

class AuthenticationApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {
    @MockitoBean
    lateinit var jwtService: JwtService

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val revokedAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                revoked = true,
                validTill = MOCK_TIME.plus(Duration.ofDays(100)),
                token = "revokedToken"
            )
            val expiredAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                revoked = false,
                validTill = MOCK_TIME.minusMillis(1),
                token = "expiredToken"
            )
            val validAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                revoked = false,
                validTill = MOCK_TIME.plus(Duration.ofDays(42)),
                token = "validToken"
            )
        }
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is not known`() {
        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=42")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is revoked`() {
        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=${preconditions.revokedAccessToken.token}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is expired`() {
        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=${preconditions.expiredAccessToken.token}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return a JWT token for valid workspace access token`() {
        // Force preconditions evaluation before stubbing
        val tokenValue = preconditions.validAccessToken.token
        val validTill = preconditions.validAccessToken.validTill

        whenever(jwtService.buildJwtToken(argThat {
            userName == tokenValue
                    && isTransient
                    && roles.size == 1
                    && roles.contains(SaUserRoles.USER)
        }, eq(validTill))) doReturn "jwtTokenForSharedWorkspace"

        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=$tokenValue")
            .exchange()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForSharedWorkspace")
    }
}
