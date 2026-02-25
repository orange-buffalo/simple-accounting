package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.security.SaUserRoles
import io.orangebuffalo.simpleaccounting.business.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithSaMockUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

private const val LOGIN_BY_TOKEN_PATH = "/api/auth/login-by-token"
private const val TOKEN_PATH = "/api/auth/token"

class AuthenticationApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {
    @MockitoBean
    lateinit var jwtService: JwtService

    @MockitoBean
    lateinit var refreshTokensService: RefreshTokensService

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
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
    fun `should return a JWT token when token endpoint is hit and cookie is valid`() {
        runBlocking {
            val principal = createRegularUserPrincipal(preconditions.fry.userName, "", listOf(SaUserRoles.USER))

            whenever(jwtService.buildJwtToken(principal)) doReturn "jwtTokenForFry"
            whenever(refreshTokensService.validateTokenAndBuildUserDetails("refreshTokenForFry")) doReturn principal

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .cookie("refreshToken", "refreshTokenForFry")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForFry")
        }
    }

    @Test
    @WithMockFryUser
    fun `should return a JWT token when token endpoint is hit and user is authenticated with regular user`() {
        runBlocking {
            whenever(jwtService.buildJwtToken(argThat {
                userName == preconditions.fry.userName
            }, eq(null))) doReturn "jwtTokenForFry"

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForFry")

            verifyNoMoreInteractions(refreshTokensService)
        }
    }

    @Test
    @WithSaMockUser(transient = true, workspaceAccessToken = "validToken")
    fun `should return a JWT token when token endpoint is hit and user is authenticated with transient user`() {
        runBlocking {
            // Force preconditions evaluation before stubbing
            val validTill = preconditions.validAccessToken.validTill

            whenever(jwtService.buildJwtToken(argThat {
                userName == "validToken"
            }, eq(validTill))) doReturn "jwtTokenForTransientUser"

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForTransientUser")

            verifyNoMoreInteractions(refreshTokensService)
        }
    }

    @Test
    fun `should return 401 if refresh token is not valid and user is not authenticated`() {
        runBlocking {
            whenever(
                refreshTokensService.validateTokenAndBuildUserDetails("refreshTokenForFry")
            ) doThrow BadCredentialsException("")

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .cookie("refreshToken", "refreshTokenForFry")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    @Test
    fun `should return 401 if refresh token is missing and user is not authenticated`() {
        runBlocking {
            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
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
