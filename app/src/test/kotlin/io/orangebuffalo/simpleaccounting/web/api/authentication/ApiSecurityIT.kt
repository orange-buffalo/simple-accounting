package io.orangebuffalo.simpleaccounting.web.api.authentication

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.services.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.User
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private const val API_PATH = "/api"

@SimpleAccountingIntegrationTest
@DisplayName("When requesting $API_PATH, ")
class ApiAuthenticationIT(
    @Autowired val client: WebTestClient,
) {

    @MockBean
    lateinit var jwtService: JwtService

    @Test
    fun `should return 401 if token is missing`() {
        client.post().uri(API_PATH)
            .contentType(APPLICATION_JSON)
            .verifyUnauthorized()
            .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
    }

    @Test
    fun `should return 401 if token is broken`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doThrow BadCredentialsException("Bad token")

        client.post().uri(API_PATH)
            .header("Authorization", "Bearer token")
            .contentType(APPLICATION_JSON)
            .verifyUnauthorized()
            .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
    }

    @Test
    fun `should return successful response if token belongs to an admin`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doReturn User.builder()
            .roles("ADMIN")
            .username("Professor Farnsworth")
            .password("token")
            .build()

        client.get().uri("$API_PATH/users/test")
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should return successful response if token belongs to a user`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doReturn User.builder()
            .roles("USER")
            .username("Fry")
            .password("token")
            .build()

        client.get().uri("$API_PATH/non-admin-endpoint")
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus().isOk
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testRestController(): ApiSecurityTestController = ApiSecurityTestController()
    }

    @RestController
    class ApiSecurityTestController {
        @GetMapping("$API_PATH/users/test")
        fun adminEndpoint(): String = "hello test admin"

        @GetMapping("$API_PATH/non-admin-endpoint")
        fun regularUserEndpoint(): String = "hello test user"
    }
}
