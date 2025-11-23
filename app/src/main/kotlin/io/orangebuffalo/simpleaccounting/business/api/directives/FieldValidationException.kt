package io.orangebuffalo.simpleaccounting.business.api.directives

import graphql.ErrorClassification
import graphql.ErrorType
import io.orangebuffalo.simpleaccounting.infra.graphql.SaGrapQlErrorType
import io.orangebuffalo.simpleaccounting.infra.graphql.SaGrapQlException

/**
 * Exception thrown when field validation fails in a GraphQL directive.
 * This exception contains details about which fields failed validation and why.
 */
class FieldValidationException(
    message: String,
    val validationErrors: List<FieldValidationError>,
    errorClassification: ErrorClassification = ErrorType.ValidationError,
) : SaGrapQlException(
    message = message,
    errorType = SaGrapQlErrorType.FIELD_VALIDATION_FAILURE,
    errorClassification = errorClassification
)

/**
 * Details about a single field validation failure.
 * This structure matches the REST API constraint violations format for consistency.
 */
data class FieldValidationError(
    val field: String,
    val error: String,
    val message: String,
    val params: Map<String, String>? = null
)
