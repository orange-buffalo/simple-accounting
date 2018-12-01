package io.orangebuffalo.accounting.simpleaccounting.web.api

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private const val PATH_VALIDATION_EXCEPTION = "/api/v1/auth/api-controller-advice-test-validation"
private const val PATH_ENTITY_NOT_FOUND_EXCEPTION = "/api/v1/auth/api-controller-advice-test-not-found"

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Any API controller")
internal class RestApiControllerAdviceIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should return 400 and a valid message when ApiValidationException is thrown`() {
        client.get().uri(PATH_VALIDATION_EXCEPTION)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>().isEqualTo("Invalid request")
    }

    @Test
    fun `should return 404 and a valid message when EntityNotFoundException is thrown`() {
        client.get().uri(PATH_ENTITY_NOT_FOUND_EXCEPTION)
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Space Bees not found")
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testRestController(): ApiControllerAdviceTestController = ApiControllerAdviceTestController()
    }

    @RestController
    class ApiControllerAdviceTestController {
        @GetMapping(PATH_VALIDATION_EXCEPTION)
        fun testValidationException(): String {
            throw ApiValidationException("Invalid request")
        }

        @GetMapping(PATH_ENTITY_NOT_FOUND_EXCEPTION)
        fun testEntityNotFoundException(): String {
            throw EntityNotFoundException("Space Bees not found")
        }
    }
}