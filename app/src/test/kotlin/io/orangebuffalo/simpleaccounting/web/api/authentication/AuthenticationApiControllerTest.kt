package io.orangebuffalo.simpleaccounting.web.api.authentication

import com.nhaarman.mockitokotlin2.*
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.expectThatJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.expectThatJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.security.WithSaMockUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokenService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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
private const val LOGIN_BY_TOKEN_PATH = "/api/auth/login-by-token"
private const val TOKEN_PATH = "/api/auth/token"

@SimpleAccountingIntegrationTest
class AuthenticationApiControllerTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val timeService: TimeService,
    preconditionsFactory: PreconditionsFactory,
) {
    @MockBean
    lateinit var jwtService: JwtService

    @MockBean
    lateinit var refreshTokenService: RefreshTokenService

    private val preconditions by preconditionsFactory {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val inactiveUser = platformUser(
                userName = "Inactive",
                activated = false
            )
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
    fun `should return a JWT token for valid user login credentials`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            userName == preconditions.fry.userName
                    && roles.size == 1
                    && roles.contains("USER")
        }, eq(null))) doReturn "jwtTokenForFry"

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = preconditions.fry.userName,
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
    fun `should return a JWT token for valid admin login credentials`() {
        whenever(passwordEncoder.matches("$&#@(@", preconditions.farnsworth.passwordHash)) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            userName == preconditions.farnsworth.userName
                    && roles.size == 1
                    && roles.contains("ADMIN")
        }, eq(null))) doReturn "jwtTokenForFarnsworth"

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = preconditions.farnsworth.userName,
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
    fun `should return 401 when password does not match`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn false

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = preconditions.fry.userName,
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
    fun `should return 401 when user is not activated`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = preconditions.inactiveUser.userName,
                    password = "irrelevant"
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
            .expectThatJsonBodyEqualTo {
                put("error", "InvalidInput")
                putJsonArray("requestErrors") {
                    addJsonObject {
                        put("field", "userName")
                        put("error", "MustNotBeNull")
                        put("message", "must not be null")
                    }
                }
            }
    }

    @Test
    fun `should return 400 on non-json request body`() {
        @Suppress("JsonStandardCompliance")
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
    fun `should return a refresh token for valid user login credentials if remember me requested`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            userName == preconditions.fry.userName
                    && roles.size == 1
                    && roles.contains("USER")
        }, eq(null))) doReturn "jwtTokenForFry"

        runBlocking {
            whenever(refreshTokenService.generateRefreshToken(preconditions.fry.userName)) doReturn "refreshTokenForFry"
        }

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(
                LoginRequest(
                    userName = preconditions.fry.userName,
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
    fun `should return a JWT token when token endpoint is hit and cookie is valid`() {
        runBlocking {
            val principal = createRegularUserPrincipal(preconditions.fry.userName, "", listOf("USER"))

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

            verifyNoMoreInteractions(refreshTokenService)
        }
    }

    @Test
    @WithSaMockUser(transient = true, workspaceAccessToken = "validToken")
    fun `should return a JWT token when token endpoint is hit and user is authenticated with transient user`() {
        runBlocking {
            mockCurrentTime(timeService)

            whenever(jwtService.buildJwtToken(argThat {
                userName == "validToken"
            }, eq(preconditions.validAccessToken.validTill))) doReturn "jwtTokenForTransientUser"

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
    fun `should return 401 if refresh token is not valid and user is not authenticated`() {
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
        mockCurrentTime(timeService)

        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=42")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is revoked`() {
        mockCurrentTime(timeService)

        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=${preconditions.revokedAccessToken.token}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 on shared workspaces token login if token is expired`() {
        mockCurrentTime(timeService)

        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=${preconditions.expiredAccessToken.token}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return a JWT token for valid workspace access token`() {
        mockCurrentTime(timeService)

        whenever(jwtService.buildJwtToken(argThat {
            userName == preconditions.validAccessToken.token
                    && isTransient
                    && roles.size == 1
                    && roles.contains("USER")
        }, eq(preconditions.validAccessToken.validTill))) doReturn "jwtTokenForSharedWorkspace"

        client.post().uri("$LOGIN_BY_TOKEN_PATH?sharedWorkspaceToken=${preconditions.validAccessToken.token}")
            .exchange()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForSharedWorkspace")
    }
}
