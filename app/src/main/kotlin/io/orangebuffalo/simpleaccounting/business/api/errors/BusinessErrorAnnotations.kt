package io.orangebuffalo.simpleaccounting.business.api.errors

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection
import kotlin.reflect.KClass

/**
 * Maps an exception thrown by a GraphQL mutation/query to a business error code.
 * The system will:
 * 1. Generate a schema directive that describes the business errors returned by this operation.
 * 2. Generate an enum type `<OperationName>ErrorCodes` containing all error codes for this operation.
 * 3. Handle runtime exceptions declared in the annotations and transform them to GraphQL errors
 *    with the specified error code.
 *
 * Each operation can have multiple [BusinessError] annotations to declare multiple error mappings.
 * The enum types are dynamically generated based on the operation name using the convention
 * `<OperationName>ErrorCodes` (e.g., `ChangePasswordErrorCodes` for the `changePassword` mutation).
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
     * The error code to return in the GraphQL error response.
     * This will be included in the schema directive, in a dynamically generated enum type,
     * and in the error extensions at runtime.
     */
    @param:GraphQLDescription("The error code that will be returned in the GraphQL error response.")
    val errorCode: String,
    /**
     * Description of the error code for the GraphQL schema documentation.
     */
    val description: String = "",
    /**
     * Optional type for additional error extensions that will be included in the GraphQL error response.
     * When specified, the type will be registered as an additional type in the GraphQL schema
     * for documentation and client code generation purposes.
     * The exception class must implement [GraphQlBusinessErrorExtensionsProvider] to provide
     * the extensions at runtime.
     */
    val extensionsType: KClass<*> = Unit::class,
)
