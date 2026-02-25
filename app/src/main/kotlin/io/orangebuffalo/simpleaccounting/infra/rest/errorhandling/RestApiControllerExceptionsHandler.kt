package io.orangebuffalo.simpleaccounting.infra.rest.errorhandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.security.InsufficientUserType
import io.orangebuffalo.simpleaccounting.business.workspaces.InvalidWorkspaceAccessTokenException
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
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
import java.lang.reflect.Method
import java.util.*
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
    fun onException(exception: ServerWebInputException): Mono<ResponseEntity<*>> {
        logger.trace(exception) { "Bad request to ${exception.methodParameter}" }
        return handleNestedRuntimeException(exception)
    }

    @ExceptionHandler
    fun onException(exception: CodecException): Mono<ResponseEntity<*>> {
        logger.trace(exception) { }
        return handleNestedRuntimeException(exception)
    }

    private fun handleNestedRuntimeException(exception: NestedRuntimeException): Mono<ResponseEntity<*>> {
        val response = when (val cause = exception.mostSpecificCause) {
            is MismatchedInputException -> handleMismatchedInputException(cause)
            else -> GeneralErrorDto("Bad JSON request")
        }
        return jsonBadRequest(response)
    }

    private fun handleMismatchedInputException(cause: MismatchedInputException): Any {
        if (cause.message?.contains("which is a non-nullable type") == true
            || cause.message?.contains("FAIL_ON_NULL_FOR_PRIMITIVES to 'false'") == true
            || cause.message?.contains("Missing required creator property") == true
        ) {
            val fieldName = cause.path.joinToString(".") { it.fieldName }
            return InvalidInputErrorDto(
                requestErrors = listOf(
                    InvalidInputErrorDto.FieldErrorDto(
                        field = fieldName,
                        error = "MustNotBeNull",
                        message = "must not be null"
                    )
                )
            )
        } else return GeneralErrorDto("Bad JSON request")
    }

    @ExceptionHandler
    fun onException(exception: WebExchangeBindException): Mono<ResponseEntity<*>> {
        logger.trace(exception) {}

        val fieldErrors = exception.bindingResult.allErrors
            .map { error ->
                val fieldName = if (error is FieldError) error.field else "<unknown>"
                val message = error.defaultMessage ?: "<not provided>"
                val springError = error.code ?: "<unknown>"
                val mapping = springValidationErrorsMappings[springError]
                if (mapping != null) {
                    InvalidInputErrorDto.FieldErrorDto(
                        field = fieldName,
                        error = mapping.error,
                        message = message,
                        // argument[0] is the object itself
                        params = if ((error.arguments?.size ?: 0) > 1) {
                            mapping.paramsConverter(error.arguments!!.copyOfRange(1, error.arguments!!.size))
                        } else null
                    )
                } else {
                    logger.warn { "No mapping is registered for $springError" }
                    InvalidInputErrorDto.FieldErrorDto(
                        field = fieldName,
                        error = springError,
                        message = message
                    )
                }
            }

        return jsonBadRequest(
            InvalidInputErrorDto(
                requestErrors = fieldErrors
            )
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

    private fun jsonBadRequest(body: Any): Mono<ResponseEntity<*>> = Mono.just(
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
    )
}

data class GeneralErrorDto(
    val error: String
)

open class SaApiErrorDto<T : Enum<T>>(
    val error: T,
    val message: String? = null,
)

data class InvalidInputErrorDto(
    val error: String = "InvalidInput",
    val requestErrors: List<FieldErrorDto>
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class FieldErrorDto(
        val field: String,
        val error: String,
        val message: String,
        val params: Map<String, String>? = null,
    )
}

/**
 * A base class for complex cases of API error handing when response body is more than just an error code.
 * @see HandleApiErrorsWith
 */
abstract class ComplexResponseBodyErrorHandler<E : Enum<E>, R : SaApiErrorDto<E>>(
    private val responseType: KClass<R>,
    private val exceptionMappings: Map<KClass<out Exception>, E>
) : ApiErrorHandler<R> {

    override fun getHttpStatus(): HttpStatus = HttpStatus.BAD_REQUEST

    override fun getResponseBodyDescriptor(): ApiErrorResponseBodyDescriptor =
        ApiErrorResponseBodyDescriptor.ofClass(responseType)

    override fun handleApiError(exception: Throwable): R? {
        val error = exceptionMappings[exception::class]
        return if (error == null) null else {
            responseType.primaryConstructor?.call(error, exception.message)
                ?: throw IllegalStateException("Cannot instantiate $responseType")
        }
    }
}

/**
 * API error handler for the standard cases with simple enum-based error response bodies.
 * @see ApiErrorMapping
 */
internal class SimpleApiErrorHandler(
    private val apiErrorMappings: Array<ApiErrorMapping>,
    private val endpointMethod: Method,
) : ApiErrorHandler<SimpleApiErrorDto> {
    override fun getHttpStatus(): HttpStatus = HttpStatus.BAD_REQUEST

    override fun getResponseBodyDescriptor(): ApiErrorResponseBodyDescriptor {
        val apiName = endpointMethod.declaringClass.simpleName.removeSuffix("Controller")
        val endpointNameCapitalized = endpointMethod.name.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        return ApiErrorResponseBodyDescriptor(
            typeName = "${apiName}${endpointNameCapitalized}Errors",
            schemaProvider = {
                // SimpleApiErrorDto but with "dynamic" enum for error field
                ObjectSchema()
                    .required(listOf(SimpleApiErrorDto::error.name))
                    .addProperty(
                        SimpleApiErrorDto::error.name,
                        StringSchema()
                            ._enum(apiErrorMappings.map { it.apiError }),
                    )
                    .addProperty(
                        SimpleApiErrorDto::message.name,
                        StringSchema(),
                    )
            }
        )
    }

    override fun handleApiError(exception: Throwable): SimpleApiErrorDto? {
        val error = apiErrorMappings.firstOrNull { it.exceptionClass.isInstance(exception) }?.apiError
        return if (error == null) null else {
            SimpleApiErrorDto(
                error = error,
                message = exception.message,
            )
        }
    }
}

/**
 * Same structure as [SaApiErrorDto] but not imposing enum constraint.
 */
internal class SimpleApiErrorDto(
    val error: String,
    val message: String? = null,
)

private data class SpringValidationErrorMapping(
    val error: String,
    val paramsConverter: (Array<Any>) -> Map<String, String> = { emptyMap() }
)

private val springValidationErrorsMappings = mapOf(
    "NotBlank" to SpringValidationErrorMapping("MustNotBeBlank"),
    "Size" to SpringValidationErrorMapping("SizeConstraintViolated") { args ->
        mapOf("min" to args[1].toString(), "max" to args[0].toString())
    }
)
