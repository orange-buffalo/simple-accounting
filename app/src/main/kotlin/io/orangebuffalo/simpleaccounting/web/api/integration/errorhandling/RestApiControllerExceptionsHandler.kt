package io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.orangebuffalo.simpleaccounting.services.business.InvalidWorkspaceAccessTokenException
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.security.InsufficientUserType
import io.orangebuffalo.simpleaccounting.services.security.authentication.AccountIsTemporaryLockedException
import io.orangebuffalo.simpleaccounting.services.security.authentication.LoginUnavailableException
import mu.KotlinLogging
import org.springframework.core.NestedRuntimeException
import org.springframework.core.codec.CodecException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

private val logger = KotlinLogging.logger {}

// TODO #906: split this class
@ControllerAdvice(basePackages = ["io.orangebuffalo.simpleaccounting"])
internal class RestApiControllerExceptionsHandler(
    private val apiErrorsRegistry: ApiErrorsRegistry,
) {

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
    fun onException(exception: AccessDeniedException): Mono<ResponseEntity<GeneralErrorDto>> {
        logger.trace(exception) {}
        return Mono.just(
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeneralErrorDto("AccessDenied"))
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
    fun onException(exception: Throwable, webExchange: ServerWebExchange): Mono<ResponseEntity<*>> {
        apiErrorsRegistry.errorDescriptors.forEach { errorDescriptor ->
            if (errorDescriptor.patternsCondition.getMatchingCondition(webExchange) != null) {
                val error = errorDescriptor.errorHandler.handleApiError(exception)
                if (error != null) {
                    return Mono.just(
                        ResponseEntity.status(errorDescriptor.responseStatus)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(error)
                    )
                }
            }
        }

        logger.error(exception) { "Something bad happened" }
        return Mono.just(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeneralErrorDto("We could not process your request"))
        )
    }

    @ExceptionHandler
    fun onException(exception: ServerWebInputException): Mono<ResponseEntity<String>> {
        logger.trace(exception) { "Bad request to ${exception.methodParameter}" }
        return handleNestedRuntimeException(exception)
    }

    @ExceptionHandler
    fun onException(exception: CodecException): Mono<ResponseEntity<String>> {
        logger.trace(exception) { }
        return handleNestedRuntimeException(exception)
    }

    private fun handleNestedRuntimeException(exception: NestedRuntimeException): Mono<ResponseEntity<String>> {
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

open class SaApiErrorDto<T : Enum<T>>(
    val error: T,
    val message: String? = null,
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

abstract class DefaultErrorHandler<E : Enum<E>, R : SaApiErrorDto<E>>(
    private val responseType: KClass<R>,
    private val exceptionMappings: Map<KClass<out Exception>, E>
) : ApiErrorHandler<R> {

    override fun getHttpStatus(): HttpStatus = HttpStatus.BAD_REQUEST

    override fun getResponseType(): KClass<R> = responseType

    override fun handleApiError(exception: Throwable): R? {
        val error = exceptionMappings[exception::class]
        return if (error == null) null else {
            responseType.primaryConstructor?.call(error, exception.message)
                ?: throw IllegalStateException("Cannot instantiate $responseType")
        }
    }
}
