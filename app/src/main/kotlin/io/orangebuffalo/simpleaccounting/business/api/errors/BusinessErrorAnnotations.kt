package io.orangebuffalo.simpleaccounting.business.api.errors

import kotlin.reflect.KClass

/**
 * Maps an exception thrown by a GraphQL mutation/query to a business error code.
 * The system will:
 * 1. Generate a schema directive that describes the business errors returned by this operation.
 * 2. Handle runtime exceptions declared in the annotations and transform them to GraphQL errors
 *    with the specified error code.
 *
 * Each operation can have multiple [BusinessError] annotations to declare multiple error mappings.
 * Each operation will have its own isolated enum type in the GraphQL schema.
 *
 * @see SaDataFetcherExceptionHandler
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class BusinessError(
    /**
     * The exception class that triggers this business error.
     */
    val exceptionClass: KClass<out Exception>,
    /**
     * The error code to return in the GraphQL error response.
     * This will be included in the schema directive and in the error extensions.
     */
    val errorCode: String,
    /**
     * Description of the error for schema documentation.
     */
    val description: String = "",
)
