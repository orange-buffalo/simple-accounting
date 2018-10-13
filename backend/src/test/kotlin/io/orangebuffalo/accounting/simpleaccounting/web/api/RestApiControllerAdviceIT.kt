package io.orangebuffalo.accounting.simpleaccounting.web.api

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private const val PATH = "/api/v1/auth/test"

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Any API controller")
internal class RestApiControllerAdviceIT {

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `should return 400 and a valid message when ApiValidationException is thrown`() {
        client.get().uri(PATH)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<String>().isEqualTo("Invalid request")
    }

    @RestController
    class TestController {
        @GetMapping(PATH)
        fun test(): String {
            throw ApiValidationException("Invalid request")
        }
    }
}