package io.orangebuffalo.accounting.simpleaccounting.web.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@ControllerAdvice(basePackageClasses = [RestApiControllerAdvice::class])
class RestApiControllerAdvice {

    @ExceptionHandler
    fun onException(exception: AuthenticationException): Mono<ResponseEntity<Any>> {
        logger.error(exception) { "Authentication exception " }
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
    }

    @ExceptionHandler
    fun onException(exception: Throwable): Mono<ResponseEntity<Any>> {
        logger.error(exception) { "Something bad happened " }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }

}