package io.orangebuffalo.simpleaccounting.business.api.errors

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection
import kotlin.reflect.KClass

/**
 * Marker interface for business error code enums.
 * Each enum that represents business error codes should implement this interface.
 */
interface BusinessErrorCode

/**
 * Maps an exception thrown by a GraphQL mutation/query to a business error code.
 * The system will:
 * 1. Generate a schema directive that describes the business errors returned by this operation.
 * 2. Handle runtime exceptions declared in the annotations and transform them to GraphQL errors
 *    with the specified error code.
 *
 * Each operation can have multiple [BusinessError] annotations to declare multiple error mappings.
 * Each operation should define its own enum type for error codes that implements [BusinessErrorCode].
 * The enum class must be added to the `additionalTypes` in the GraphQL schema configuration.
 *
 * @see SaDataFetcherExceptionHandler
 */
@GraphQLDirective(
    name = "businessError",
    description = "Declares a business error that can be returned by this operation. " +
            "When the specified exception is thrown, the response will include an error with " +
            "extensions.errorType = 'BUSINESS_ERROR' and extensions.errorCode containing the declared error code.",
    locations = [Introspection.DirectiveLocation.FIELD_DEFINITION]
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class BusinessError(
    /**
     * The exception class that triggers this business error.
     */
    val exceptionClass: KClass<out Exception>,
    /**
     * The enum class containing the error code.
     * This should be an enum that implements [BusinessErrorCode].
     * The enum must be added to the GraphQL schema via `additionalTypes`.
     * This parameter ensures type safety - use constants from this enum for [errorCode].
     */
    val errorCodeClass: KClass<out BusinessErrorCode>,
    /**
     * The error code name (must match an enum constant in [errorCodeClass]).
     * This will be included in the schema directive and in the error extensions.
     */
    @param:GraphQLDescription("The error code that will be returned in the GraphQL error response.")
    val errorCode: String,
)
