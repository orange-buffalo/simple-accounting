package io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling

import org.springframework.http.HttpStatus
import kotlin.reflect.KClass

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
     * Type of the response body when this handler handles an error. Primarily used for schema generation.
     */
    fun getResponseType(): KClass<R>

    /**
     * Handles an error. If error is handled, returns a non-null response. If error type is not supported, returns null.
     */
    fun handleApiError(exception: Throwable): R?
}
