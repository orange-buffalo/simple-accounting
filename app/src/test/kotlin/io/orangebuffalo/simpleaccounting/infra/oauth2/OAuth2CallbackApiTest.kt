package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.expectThatJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.tests.infra.api.shouldBeEqualToJson
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

class OAuth2CallbackApiTest(
    @Autowired private val client: WebTestClient
) : SaIntegrationTestBase() {

    @MockitoBean
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
                shouldBeEqualToJson(
                    """{
                        "errorId": "${JsonValues.ANY_STRING}"    
                    }"""
                )
            }
    }
}
