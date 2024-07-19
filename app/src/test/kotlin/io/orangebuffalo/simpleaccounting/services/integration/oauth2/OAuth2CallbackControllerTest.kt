package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import com.nhaarman.mockitokotlin2.*
import io.orangebuffalo.simpleaccounting.infra.api.expectThatJsonBody
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
class OAuth2CallbackControllerTest(
    @Autowired private val client: WebTestClient
) {

    @MockBean
    lateinit var authorizationProvider: OAuth2ClientAuthorizationProvider

    @Test
    fun `should fail on authorization callback when state is missing`() {
        client.post()
            .uri("/api/auth/oauth2/callback")
            .sendJson(
                """{
                    "code": null,
                    "error": null,
                    "state": null
                }"""
            )
            .exchange()
            .expectStatus().isBadRequest

        verifyNoMoreInteractions(authorizationProvider)
    }

    @Test
    fun `should pass the request to the authentication provider`() {
        client.post()
            .uri("/api/auth/oauth2/callback")
            .sendJson(
                """{
                    "code": "code",
                    "error": null,
                    "state": "state"
                }"""
            )
            .exchange()
            .expectStatus().isOk

        verifyBlocking(authorizationProvider) {
            handleAuthorizationResponse(
                OAuth2AuthorizationCallbackRequest(
                    code = "code",
                    error = null,
                    state = "state"
                )
            )
        }
    }

    @Test
    fun `should pass the request to the authentication provider and handle errors`() {
        authorizationProvider.stub {
            onBlocking { handleAuthorizationResponse(any()) } doThrow IllegalStateException("Something bad")
        }

        client.post()
            .uri("/api/auth/oauth2/callback")
            .sendJson(
                """{
                    "code": "code",
                    "error": null,
                    "state": "state"
                }"""
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectThatJsonBody {
                json(
                    """{
                        "errorId": "#{json-unit.any-string}"    
                    }"""
                )
            }
    }
}
