package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class JwtAuthenticationFilterIT(
    @Autowired val client: WebTestClient
) {

    @MockBean
    lateinit var jwtService: JwtService

    @Test
    fun `When requesting non-guarded path, should not fail if token is broken`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doThrow BadCredentialsException("Bad token")

        client.post().uri("/api/v1/auth/non-guarded")
            .header("Authorization", "Bearer token")
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }
}