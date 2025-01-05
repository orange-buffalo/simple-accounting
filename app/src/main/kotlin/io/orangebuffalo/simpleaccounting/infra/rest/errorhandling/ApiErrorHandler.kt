package io.orangebuffalo.simpleaccounting.infra.rest.errorhandling

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.media.Schema
import org.springframework.http.HttpStatus
import kotlin.reflect.KClass

/**
 * Maps an exception thrown by an API endpoint to a response body error type.
 * The system will collect all annotations on an endpoint and create an OpenAPI schema component
 * with enum type for all specified errors. The structure of the error response is same as [SaApiErrorDto].
 * The corresponding error handler with reply with scheme-compatible JSON.
 *
 * This is recommended business error handling declaration unless more complex response types are required.
 * In the latter case, use [HandleApiErrorsWith] which allows to declare custom complex types.
 *
 * @see RestApiControllerExceptionsHandler
 * @see SaOpenApiCustomizer
 * @see ApiErrorsRegistry
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ApiErrorMapping(
    val exceptionClass: KClass<out Exception>,
    val apiError: String,
)

/**
 * Declares an error handler on the API endpoint to be used both for producing error responses
 * (see [RestApiControllerExceptionsHandler]) and OpenAPI schema (see [SaOpenApiCustomizer]).
 *
 * This annotation is only recommended for complex response types. For standard cases, use [ApiErrorMapping].
 *
 * @see ApiErrorsRegistry for discover and assembly
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class HandleApiErrorsWith(
    /**
     * Error handler class to be used for this endpoint. Typically, implementations extend [ComplexResponseBodyErrorHandler].
     */
    val errorHandler: KClass<out ApiErrorHandler<*>>,
)

/**
 * An extension to Spring's exception handling for REST controllers that allows to reuse a single definition of
 * error handling for both controller advice and OpenAPI schema generation
 * (see limitations described in [SaOpenApiCustomizer]).
 *
 * The implementation can be instantiated at any point in time, more than once. Should be stateless and make no
 * assumptions about the order for invocations.
 *
 * @see SaOpenApiCustomizer
 * @see RestApiControllerExceptionsHandler
 * @see HandleApiErrorsWith
 */
interface ApiErrorHandler<R : Any> {
    /**
     * HTTP Status to provide when this error is handled.
     */
    fun getHttpStatus(): HttpStatus

    /**
     * Descriptor of the response body when this handler handles an error, for OpenAPI schema generation.
     */
    fun getResponseBodyDescriptor(): ApiErrorResponseBodyDescriptor

    /**
     * Handles an error. If error is handled, returns a non-null response. If error type is not supported, returns null.
     */
    fun handleApiError(exception: Throwable): R?
}

/**
 * Describes the error response body for schema generation.
 */
data class ApiErrorResponseBodyDescriptor(
    val typeName: String,
    // delay creation to let Spring Doc set up the resolvers, e.g. Kotlin support
    val schemaProvider: () -> Schema<*>
) {
    companion object {
        fun ofClass(klass: KClass<*>): ApiErrorResponseBodyDescriptor {
            return ApiErrorResponseBodyDescriptor(
                typeName = klass.simpleName ?: throw IllegalArgumentException("Cannot get simple name for $klass"),
                schemaProvider = {
                    ModelConverters.getInstance(true)
                        .resolveAsResolvedSchema(AnnotatedType(klass.java))
                        .schema
                }
            )
        }
    }
}
