package io.orangebuffalo.simpleaccounting.web.api.integration

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.orangebuffalo.simpleaccounting.services.business.InvalidWorkspaceAccessTokenException
import io.orangebuffalo.simpleaccounting.services.security.InsufficientUserType
import io.orangebuffalo.simpleaccounting.services.security.login.AccountIsTemporaryLockedException
import io.orangebuffalo.simpleaccounting.services.security.login.LoginUnavailableException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@ControllerAdvice(basePackages = ["io.orangebuffalo.simpleaccounting"])
class RestApiControllerAdvice {

    @ExceptionHandler
    fun onException(exception: BadCredentialsException): Mono<ResponseEntity<GeneralErrorDto>> {
        logger.trace(exception) {}
        return Mono.just(
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeneralErrorDto("BadCredentials"))
        )
    }

    @ExceptionHandler
    fun onException(exception: LoginUnavailableException): Mono<ResponseEntity<GeneralErrorDto>> {
        logger.trace(exception) {}
        return Mono.just(
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeneralErrorDto("LoginNotAvailable"))
        )
    }

    @ExceptionHandler
    fun onException(exception: AccountIsTemporaryLockedException):
            Mono<ResponseEntity<AccountIsTemporaryLockedErrorDto>> {
        logger.trace(exception) {}
        return Mono.just(
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(exception.toDto())
        )
    }

    @ExceptionHandler
    fun onException(exception: AuthenticationException): Mono<ResponseEntity<Any>> {
        logger.trace(exception) {}
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
    }

    @ExceptionHandler
    fun onException(exception: Throwable): Mono<ResponseEntity<GeneralErrorDto>> {
        logger.error(exception) { "Something bad happened" }
        return Mono.just(
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeneralErrorDto("UnauthorizedError"))
        )
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
        logger.trace(exception) {}

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
        logger.trace(exception) {}

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.message)
        )
    }

    @ExceptionHandler
    fun onException(exception: EntityNotFoundException): Mono<ResponseEntity<String>> {
        logger.trace(exception) {}

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception.message)
        )
    }

    @ExceptionHandler
    fun onException(exception: InvalidWorkspaceAccessTokenException): Mono<ResponseEntity<String>> {
        logger.trace(exception) {}

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.message)
        )
    }

    @ExceptionHandler
    fun onException(exception: InsufficientUserType): Mono<ResponseEntity<String>> {
        logger.trace(exception) {}

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .build()
        )
    }
}

data class GeneralErrorDto(
    val error: String
)

@Suppress("unused")
class AccountIsTemporaryLockedErrorDto(
    val error: String = "AccountLocked",
    val lockExpiresInSec: Long
)

private fun AccountIsTemporaryLockedException.toDto() =
    AccountIsTemporaryLockedErrorDto(
        lockExpiresInSec = lockExpiresInSec
    )

