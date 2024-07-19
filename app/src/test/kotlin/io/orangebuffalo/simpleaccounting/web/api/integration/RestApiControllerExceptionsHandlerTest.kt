package io.orangebuffalo.simpleaccounting.web.api.integration

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.security.InsufficientUserType
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.ApiValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private const val PATH_VALIDATION_EXCEPTION = "/api/auth/api-controller-advice-test-validation"
private const val PATH_ENTITY_NOT_FOUND_EXCEPTION = "/api/auth/api-controller-advice-test-not-found"
private const val PATH_INSUFFICIENT_USER_TYPE_EXCEPTION = "/api/auth/api-controller-advice-test-insufficient-user-type"

@SimpleAccountingIntegrationTest
@DisplayName("Any API controller")
internal class RestApiControllerExceptionsHandlerTest(
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
            .verifyNotFound("Space Bees not found")
    }

    @Test
    fun `should return 400 when InsufficientUserType is thrown`() {
        client.get().uri(PATH_INSUFFICIENT_USER_TYPE_EXCEPTION)
            .exchange()
            .expectStatus().isBadRequest
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testRestController(): ApiControllerAdviceTestController =
            ApiControllerAdviceTestController()
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

        @GetMapping(PATH_INSUFFICIENT_USER_TYPE_EXCEPTION)
        fun testInsufficientUserTypeException(): String {
            throw InsufficientUserType()
        }
    }
}
