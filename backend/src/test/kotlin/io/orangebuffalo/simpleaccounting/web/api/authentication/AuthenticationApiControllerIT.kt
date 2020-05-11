package io.orangebuffalo.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.*
import io.orangebuffalo.simpleaccounting.*
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.services.security.remeberme.RefreshTokenService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

private const val LOGIN_PATH = "/api/auth/login"
private const val TOKEN_PATH = "/api/auth/token"

@SimpleAccountingIntegrationTest
class AuthenticationApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @MockBean
    lateinit var passwordEncoder: PasswordEncoder

    @MockBean
    lateinit var jwtService: JwtService

    @MockBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockBean
    lateinit var timeService: TimeService

    @Test
    fun `should return a JWT token for valid user login credentials`(testData: AuthenticationApiTestData) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            userName == "Fry"
                    && roles.size == 1
                    && roles.contains("USER")
        }, eq(null))) doReturn "jwtTokenForFry"

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "Fry",
                    password = "qwerty"
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForFry")
    }

    @Test
    fun `should return a JWT token for valid admin login credentials`(testData: AuthenticationApiTestData) {
        whenever(passwordEncoder.matches("$&#@(@", "scienceBasedHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            userName == "Farnsworth"
                    && roles.size == 1
                    && roles.contains("ADMIN")
        }, eq(null))) doReturn "jwtTokenForFarnsworth"

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "Farnsworth",
                    password = "$&#@(@"
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForFarnsworth")
    }

    @Test
    fun `should return 401 when user is unknown`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "Roberto",
                    password = "qwerty"
                )
            )
            .exchange()
            .expectStatus().isUnauthorized
            .expectThatJsonBody {
                inPath("$.error").isEqualTo("BadCredentials")
            }
    }

    @Test
    fun `should return 401 when password does not match`(testData: AuthenticationApiTestData) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "Fry",
                    password = "qwerty"
                )
            )
            .exchange()
            .expectStatus().isUnauthorized
            .expectThatJsonBody {
                inPath("$.error").isEqualTo("BadCredentials")
            }
    }

    @Test
    fun `should return 400 when request body is empty`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).containsIgnoringCase("Bad JSON request")
            }
    }

    @Test
    fun `should return 400 on invalid request body`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).containsIgnoringCase("Property userName is required")
            }
    }

    @Test
    fun `should return 400 on non-json request body`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue("hello")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).containsIgnoringCase("Bad JSON request")
            }
    }

    @Test
    fun `should return 400 when username is missing in a request`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "",
                    password = "qwerty"
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody)
                    .containsIgnoringCase("username")
                    .containsIgnoringCase("must not be blank")
            }
    }

    @Test
    fun `Should return 400 when password is missing in a login request`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "Fry",
                    password = ""
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody)
                    .containsIgnoringCase("password")
                    .containsIgnoringCase("must not be blank")
            }
    }

    @Test
    fun `should return a refresh token for valid user login credentials if remember me requested`(
        testData: AuthenticationApiTestData
    ) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            userName == "Fry"
                    && roles.size == 1
                    && roles.contains("USER")
        }, eq(null))) doReturn "jwtTokenForFry"

        runBlocking {
            whenever(refreshTokenService.generateRefreshToken("Fry")) doReturn "refreshTokenForFry"
        }

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = "Fry",
                    password = "qwerty",
                    rememberMe = true
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectHeader().value(HttpHeaders.SET_COOKIE) { cookie ->
                assertThat(cookie).contains("refreshToken=refreshTokenForFry")
                    .contains("Max-Age=2592000")
                    .contains("Path=/api/auth/token")
                    .contains("HttpOnly")
                    .contains("SameSite=Strict")
            }
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForFry")
    }

    @Test
    fun `should return a JWT token when token endpoint is hit and cookie is valid`(
        testData: AuthenticationApiTestData
    ) {
        runBlocking {
            val principal = createRegularUserPrincipal("Fry", "", listOf("USER"))

            whenever(jwtService.buildJwtToken(principal)) doReturn "jwtTokenForFry"
            whenever(refreshTokenService.validateTokenAndBuildUserDetails("refreshTokenForFry")) doReturn principal

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
    fun `should return a JWT token when token endpoint is hit and user is authenticated with regular user`(
        testData: AuthenticationApiTestData
    ) {
        runBlocking {
            whenever(jwtService.buildJwtToken(argThat {
                userName == "Fry"
            }, eq(null))) doReturn "jwtTokenForFry"

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForFry")

            verifyNoMoreInteractions(refreshTokenService)
        }
    }

    @Test
    @WithSaMockUser(transient = true, workspaceAccessToken = "validToken")
    fun `should return a JWT token when token endpoint is hit and user is authenticated with transient user`(
        testData: AuthenticationApiTestData
    ) {
        runBlocking {
            mockCurrentTime(timeService)

            whenever(jwtService.buildJwtToken(argThat {
                userName == "validToken"
            }, eq(testData.validAccessToken.validTill))) doReturn "jwtTokenForTransientUser"

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForTransientUser")

            verifyNoMoreInteractions(refreshTokenService)
        }
    }

    @Test
    fun `should return 401 if refresh token is not valid and user is not authenticated`(
        testData: AuthenticationApiTestData
    ) {
        runBlocking {
            whenever(
                refreshTokenService.validateTokenAndBuildUserDetails("refreshTokenForFry")
            ) doThrow BadCredentialsException("")

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .cookie("refreshToken", "refreshTokenForFry")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    @Test
    fun `should return 401 if refresh token is missing and user is not authenticated`(
        testData: AuthenticationApiTestData
    ) {
        runBlocking {
            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is not known`(testData: AuthenticationApiTestData) {
        mockCurrentTime(timeService)

        client.post().uri("$LOGIN_PATH?sharedWorkspaceToken=42")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is revoked`(testData: AuthenticationApiTestData) {
        mockCurrentTime(timeService)

        client.post().uri("$LOGIN_PATH?sharedWorkspaceToken=revokedToken")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is expired`(testData: AuthenticationApiTestData) {
        mockCurrentTime(timeService)

        client.post().uri("$LOGIN_PATH?sharedWorkspaceToken=expiredToken")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return a JWT token for valid workspace access token`(testData: AuthenticationApiTestData) {
        mockCurrentTime(timeService)

        whenever(jwtService.buildJwtToken(argThat {
            userName == "validToken"
                    && isTransient
                    && roles.size == 1
                    && roles.contains("USER")
        }, eq(testData.validAccessToken.validTill))) doReturn "jwtTokenForSharedWorkspace"

        client.post().uri("$LOGIN_PATH?sharedWorkspaceToken=validToken")
            .exchange()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForSharedWorkspace")
    }

    class AuthenticationApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val revokedAccessToken = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            revoked = true,
            validTill = MOCK_TIME.plus(Duration.ofDays(100)),
            token = "revokedToken"
        )
        val expiredAccessToken = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            revoked = false,
            validTill = MOCK_TIME.minusMillis(1),
            token = "expiredToken"
        )
        val validAccessToken = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            revoked = false,
            validTill = MOCK_TIME.plus(Duration.ofDays(42)),
            token = "validToken"
        )

        override fun generateData() = listOf(
            fry, farnsworth, fryWorkspace,
            revokedAccessToken, expiredAccessToken, validAccessToken
        )
    }
}
