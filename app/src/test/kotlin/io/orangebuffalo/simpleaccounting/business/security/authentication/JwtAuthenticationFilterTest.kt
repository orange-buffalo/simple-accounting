package io.orangebuffalo.simpleaccounting.business.security.authentication

import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
class JwtAuthenticationFilterTest(
    @Autowired val client: WebTestClient
) {
    @MockitoBean
    lateinit var jwtService: JwtService

    @Test
    fun `when requesting non-guarded path, should not fail if token is broken`() {
        whenever(jwtService.validateTokenAndBuildUserDetails("token")) doThrow BadCredentialsException("Bad token")

        client.post().uri("/api/auth/non-guarded")
            .header("Authorization", "Bearer token")
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }
}
