package io.orangebuffalo.simpleaccounting.business.ui

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

internal class WebUiConfigTest(
    @Autowired val client: WebTestClient
) : SaIntegrationTestBase() {

    @Test
    fun `should serve favicon`() {
        client.get().uri("/favicon.ico")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("image/x-icon")
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isNotEmpty()
            }
    }

    @Test
    fun `should serve static resource`() {
        client.get().uri("/assets/css/test.css")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("text/css")
            .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofDays(365)))
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isNotEmpty()
            }
    }

    @Test
    fun `should serve app page without authentication and caching`() {
        client.get().uri("/")
            .accept(TEXT_HTML)
            .exchange()
            .expectStatus().isOk
            .expectHeader().cacheControl(CacheControl.noCache())
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isNotBlank()
            }
    }

    @Test
    fun `should serve routed app page without authentication and caching`() {
        client.get().uri("/workspaces")
            .accept(TEXT_HTML)
            .exchange()
            .expectStatus().isOk
            .expectHeader().cacheControl(CacheControl.noCache())
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isNotBlank()
            }
    }

    @Test
    fun `should serve routed app page with nested path without authentication and caching`() {
        client.get().uri("/workspaces/42/edit")
            .accept(TEXT_HTML)
            .exchange()
            .expectStatus().isOk
            .expectHeader().cacheControl(CacheControl.noCache())
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isNotBlank()
            }
    }
}
