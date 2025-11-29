package io.orangebuffalo.simpleaccounting.business.api.errors

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription(
    "Defines the error types that can be returned in GraphQL errors. " +
    "These error types are included in the `extensions.errorType` field of GraphQL errors."
)
enum class SaGrapQlErrorType {
    @GraphQLDescription("Indicates that the request requires authentication or the user is not authorized to perform the operation.")
    NOT_AUTHORIZED,
    @GraphQLDescription("Indicates that one or more input fields failed validation constraints.")
    FIELD_VALIDATION_FAILURE,
}
