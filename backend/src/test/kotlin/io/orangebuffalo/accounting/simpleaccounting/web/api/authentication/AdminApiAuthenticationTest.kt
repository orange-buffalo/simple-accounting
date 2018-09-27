package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.User
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("When requesting /api/admin, ")
class AdminApiAuthenticationTest {

    @MockBean
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `should return 401 if token is missing`() {
        client.post().uri("/api/admin")
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 401 if token is broken`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doThrow BadCredentialsException("Bad token")

        client.post().uri("/api/admin")
                .header("Authorization", "Bearer token")
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `should return 403 if token belongs to a user`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doReturn User.builder()
                .roles("USER")
                .username("Fry")
                .password("token")
                .build()

        client.post().uri("/api/admin")
                .header("Authorization", "Bearer token")
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden
    }

    @Test
    fun `should return successful response if token belongs to an admin`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doReturn User.builder()
                .roles("ADMIN")
                .username("Professor Farnsworth")
                .password("token")
                .build()

        client.post().uri("/api/admin")
                .header("Authorization", "Bearer token")
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound
    }

    @TestConfiguration
    class Config {

        @Bean
        fun webClient(applicationContext: ApplicationContext): WebTestClient {
            return WebTestClient.bindToApplicationContext(applicationContext).build()
        }
    }
}