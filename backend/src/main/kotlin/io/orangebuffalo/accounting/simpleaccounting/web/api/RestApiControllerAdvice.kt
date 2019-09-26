package io.orangebuffalo.accounting.simpleaccounting.web.api

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.orangebuffalo.accounting.simpleaccounting.services.business.InvalidWorkspaceAccessTokenException
import io.orangebuffalo.accounting.simpleaccounting.services.security.InsufficientUserType
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@ControllerAdvice(basePackages = ["io.orangebuffalo.accounting.simpleaccounting"])
class RestApiControllerAdvice {

    @ExceptionHandler
    fun onException(exception: AuthenticationException): Mono<ResponseEntity<Any>> {
        logger.trace { "Authentication exception $exception" }
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
    }

    @ExceptionHandler
    fun onException(exception: Throwable): Mono<ResponseEntity<Any>> {
        logger.error(exception) { "Something bad happened" }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }

    @ExceptionHandler
    fun onException(exception: ServerWebInputException): Mono<ResponseEntity<String>> {
        logger.trace(exception) { "Bad request to ${exception.methodParameter}" }

        val cause = exception.mostSpecificCause
        val message = if (cause is MissingKotlinParameterException) {
            "Property ${cause.parameter.name} is required"
        } else {
            "Bad JSON request"
        }

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message)
        )
    }

    @ExceptionHandler
    fun onException(exception: WebExchangeBindException): Mono<ResponseEntity<String>> {
        logger.trace(exception) { "Bad request ${exception.message}" }

        val cause = exception.bindingResult.allErrors.joinToString { error ->
            if (error is FieldError) {
                "${error.field} ${error.defaultMessage}"
            } else {
                error.toString()
            }
        }

        val message = if (cause.isNotEmpty()) {
            cause
        } else {
            exception.message
        }

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message)
        )
    }

    @ExceptionHandler
    fun onException(exception: ApiValidationException): Mono<ResponseEntity<String>> {
        logger.trace(exception) { "Bad request: $exception" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.message)
        )
    }

    @ExceptionHandler
    fun onException(exception: EntityNotFoundException): Mono<ResponseEntity<String>> {
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception.message)
        )
    }

    @ExceptionHandler
    fun onException(exception: InvalidWorkspaceAccessTokenException): Mono<ResponseEntity<String>> {
        logger.trace { exception }

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.message)
        )
    }

    @ExceptionHandler
    fun onException(exception: InsufficientUserType): Mono<ResponseEntity<String>> {
        logger.trace { exception }

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .build()
        )
    }
}