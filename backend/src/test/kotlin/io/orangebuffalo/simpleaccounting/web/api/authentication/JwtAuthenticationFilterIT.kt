package io.orangebuffalo.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.services.security.jwt.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
class JwtAuthenticationFilterIT(
    @Autowired val client: WebTestClient
) {

    @MockBean
    lateinit var jwtService: JwtService

    @Test
    fun `When requesting non-guarded path, should not fail if token is broken`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doThrow BadCredentialsException("Bad token")

        client.post().uri("/api/auth/non-guarded")
            .header("Authorization", "Bearer token")
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }
}
