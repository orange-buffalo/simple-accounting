package io.orangebuffalo.accounting.simpleaccounting.web.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@ControllerAdvice(basePackageClasses = [RestApiControllerAdvice::class])
class RestApiControllerAdvice {

    @ExceptionHandler
    fun onException(exception: AuthenticationException): Mono<ResponseEntity<Any>> {
        logger.warn { "Authentication exception $exception" }
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
    }

    @ExceptionHandler
    fun onException(exception: Throwable): Mono<ResponseEntity<Any>> {
        logger.error(exception) { "Something bad happened" }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }

    @ExceptionHandler
    fun onException(exception: ServerWebInputException): Mono<ResponseEntity<String>> {
        logger.info(exception) { "Bad request to ${exception.methodParameter}" }
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.message))
    }

}