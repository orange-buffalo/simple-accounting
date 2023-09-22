package io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling

import kotlin.reflect.KClass

/**
 * Declares an error handler on the API endpoint to be used both for producing error responses
 * (see [RestApiControllerExceptionsHandler]) and OpenAPI schema (see [SaOpenApiCustomizer]).
 *
 * @see ApiErrorsRegistry
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class HandleApiErrorsWith(
    val errorHandler: KClass<out ApiErrorHandler<*>>,
)
