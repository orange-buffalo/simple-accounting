package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.*
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Farnsworth
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.RefreshTokenService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

private const val LOGIN_PATH = "/api/v1/auth/login"
private const val TOKEN_PATH = "/api/v1/auth/token"

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class LoginControllerIT(
    @Autowired val client: WebTestClient
) {

    @MockBean
    lateinit var passwordEncoder: PasswordEncoder

    @MockBean
    lateinit var jwtService: JwtService

    @MockBean
    lateinit var refreshTokenService: RefreshTokenService

    @Test
    fun `should return a JWT token for valid user login credentials`(fry: Fry) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            username == "Fry"
                    && password == "qwertyHash"
                    && authorities.size == 1
                    && authorities.iterator().next().authority == "ROLE_USER"
        })) doReturn "jwtTokenForFry"

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .syncBody(
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
    fun `should return a JWT token for valid admin login credentials`(farnsworth: Farnsworth) {
        whenever(passwordEncoder.matches("$&#@(@", "scienceBasedHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            username == "Farnsworth"
                    && password == "scienceBasedHash"
                    && authorities.size == 1
                    && authorities.iterator().next().authority == "ROLE_ADMIN"
        })) doReturn "jwtTokenForFarnsworth"

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .syncBody(
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
    fun `should return 403 when user is unknown`() {
        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .syncBody(
                LoginRequest(
                    userName = "Roberto",
                    password = "qwerty"
                )
            )
            .exchange()
            .expectStatus().isForbidden
            .expectBody().isEmpty
    }

    @Test
    fun `should return 403 when password does not match`(fry: Fry) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .syncBody(
                LoginRequest(
                    userName = "Fry",
                    password = "qwerty"
                )
            )
            .exchange()
            .expectStatus().isForbidden
            .expectBody().isEmpty
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
            .syncBody("{}")
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
            .syncBody("hello")
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
            .syncBody(
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
            .syncBody(
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
    fun `should return a refresh token for valid user login credentials if remember me requested`(fry: Fry) {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            username == "Fry"
                    && password == "qwertyHash"
                    && authorities.size == 1
                    && authorities.iterator().next().authority == "ROLE_USER"
        })) doReturn "jwtTokenForFry"

        runBlocking {
            whenever(refreshTokenService.generateRefreshToken("Fry")) doReturn "refreshTokenForFry"
        }

        client.post().uri(LOGIN_PATH)
            .contentType(APPLICATION_JSON)
            .syncBody(
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
                    .contains("Path=/api/v1/auth/token")
                    .contains("HttpOnly")
                    .contains("SameSite=Strict")
            }
            .expectBody()
            .jsonPath("$.token").isEqualTo("jwtTokenForFry")
    }

    @Test
    fun `should return a jwt token when token endpoint is hit and cookie is valid`(fry: Fry) {
        runBlocking {
            val userDetails = Mockito.mock(UserDetails::class.java)

            whenever(jwtService.buildJwtToken(userDetails)) doReturn "jwtTokenForFry"
            whenever(refreshTokenService.validateTokenAndBuildUserDetails("refreshTokenForFry")) doReturn userDetails

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
    @WithMockUser(username = "Fry")
    fun `should return a jwt token when token endpoint is hit and user is authenticated`(fry: Fry) {
        runBlocking {
            whenever(jwtService.buildJwtToken(argThat {
                username == "Fry"
            })) doReturn "jwtTokenForFry"

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
    //todo should return 401? what is the spec for wrong credentials?
    fun `should return 403 if refresh token is not valid and user is not authenticated`(fry: Fry) {
        runBlocking {
            whenever(
                refreshTokenService.validateTokenAndBuildUserDetails("refreshTokenForFry")
            ) doThrow BadCredentialsException("")

            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .cookie("refreshToken", "refreshTokenForFry")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Test
    //todo should return 401
    //todo should not log warning to avoid log pollution
    fun `should return 403 if refresh token is missing and user is not authenticated`(fry: Fry) {
        runBlocking {
            client.post().uri(TOKEN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden
        }
    }
}