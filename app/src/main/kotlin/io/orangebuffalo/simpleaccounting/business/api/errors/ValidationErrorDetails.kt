package io.orangebuffalo.simpleaccounting.business.api.errors

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.fasterxml.jackson.annotation.JsonInclude

@GraphQLDescription("Details of a field validation error that occurred during input validation.")
data class ValidationErrorDetails(
    @GraphQLDescription("The path to the field that failed validation (e.g., 'currentPassword').")
    val path: String,

    @GraphQLDescription("The error code identifying the type of validation failure (e.g., 'MustNotBeBlank', 'SizeConstraintViolated').")
    val error: String,

    @GraphQLDescription("A human-readable message describing the validation failure.")
    val message: String,

    @GraphQLDescription("Additional constraint parameters if applicable (e.g., min/max values for size constraints).")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val params: List<ValidationErrorParam>? = null
)

@GraphQLDescription("A key-value pair for validation constraint parameters.")
data class ValidationErrorParam(
    @GraphQLDescription("The parameter name (e.g., 'min', 'max').")
    val name: String,

    @GraphQLDescription("The parameter value.")
    val value: String
)
