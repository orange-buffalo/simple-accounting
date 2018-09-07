package io.orangebuffalo.accounting.simpleaccounting.web.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
internal class WebUiControllerTest {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient.bindToApplicationContext(applicationContext).build()
    }

    @Test
    fun `Should serve static resources from root without authentication`() {
        client.get().uri("/favicon.ico")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .consumeWith {
                    assertThat(it.responseBody).isNotEmpty()
                }
    }

    @Test
    fun `Should serve css without authentication`() {
        client.get().uri("/static/css/some.css")
                .exchange()
                .expectStatus().isNotFound
    }

    @Test
    fun `Should serve js without authentication`() {
        client.get().uri("/static/js/some.js")
                .exchange()
                .expectStatus().isNotFound
    }

    @Test
    fun `Should serve admin page without authentication`() {
        client.get().uri("/admin")
                .accept(TEXT_HTML)
                .exchange()
                .expectStatus().isOk
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).isNotBlank()
                }
    }

    @Test
    fun `Should serve app page without authentication`() {
        client.get().uri("/app")
                .accept(TEXT_HTML)
                .exchange()
                .expectStatus().isOk
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).isNotBlank()
                }
    }

}