package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.ErrorClassification
import graphql.ErrorType
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.SourceLocation
import graphql.validation.ValidationError
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlErrorType
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Message patterns from graphql-java's i18n/Validation.properties that indicate a null or missing
 * value for a non-nullable field argument. The first capture group is the field name in all patterns.
 */
private val NULL_FIELD_PATTERNS = listOf(
    // ProvidedNonNullArguments.missingFieldArg: "Validation error (X) : Missing field argument 'fieldName'"
    Regex("""Missing field argument '([^']+)'"""),
    // ProvidedNonNullArguments.nullValue: "Validation error (X) : Null value for non-null field argument 'fieldName'"
    Regex("""Null value for non-null field argument '([^']+)'"""),
    // ArgumentValidationUtil.handleNullError: "Validation error (X) : argument 'fieldName' with value '...' must not be null"
    Regex("""argument '([^']+)' with value .+ must not be null"""),
)

/**
 * Instrumentation that transforms GraphQL schema validation errors for null or missing non-nullable
 * field arguments into the same [SaGrapQlErrorType.FIELD_VALIDATION_FAILURE] format used by
 * JSR-303 constraint violations. This provides a consistent validation error structure for API clients.
 */
@Component
class SaGraphQLNullFieldValidationInstrumentation : Instrumentation {

    override fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState?,
    ): CompletableFuture<ExecutionResult> {
        val nullFieldErrors = collectNullFieldErrors(executionResult)

        if (nullFieldErrors.isEmpty()) {
            return CompletableFuture.completedFuture(executionResult)
        }

        val handledErrors = nullFieldErrors.map { it.originalError }.toSet()
        val otherErrors = executionResult.errors.filter { it !in handledErrors }

        val transformedErrors = nullFieldErrors
            .groupBy { it.queryPath }
            .map { (queryPath, errors) ->
                val validationDetails = errors
                    .distinctBy { it.fieldName }
                    .map {
                        ValidationErrorDetails(
                            path = it.fieldName,
                            error = ValidationErrorCode.MustNotBeNull,
                            message = "must not be null",
                        )
                    }
                NullFieldValidationFailureGraphQLError(
                    queryPath = queryPath,
                    validationDetails = validationDetails,
                    sourceLocation = errors.first().originalError.locations?.firstOrNull(),
                )
            }

        return CompletableFuture.completedFuture(
            executionResult.transform { builder ->
                builder.errors(otherErrors + transformedErrors)
            }
        )
    }

    private fun collectNullFieldErrors(executionResult: ExecutionResult): List<NullFieldError> =
        executionResult.errors
            .filterIsInstance<ValidationError>()
            .mapNotNull { error ->
                val message = error.message ?: return@mapNotNull null
                val fieldName = NULL_FIELD_PATTERNS
                    .firstNotNullOfOrNull { pattern -> pattern.find(message)?.groupValues?.get(1) }
                    ?: return@mapNotNull null
                NullFieldError(
                    originalError = error,
                    fieldName = fieldName,
                    queryPath = error.queryPath ?: emptyList(),
                )
            }

    private data class NullFieldError(
        val originalError: ValidationError,
        val fieldName: String,
        val queryPath: List<String>,
    )
}

private class NullFieldValidationFailureGraphQLError(
    private val queryPath: List<String>,
    private val validationDetails: List<ValidationErrorDetails>,
    private val sourceLocation: SourceLocation?,
) : GraphQLError {

    override fun getMessage(): String = "Validation failed"

    override fun getLocations(): List<SourceLocation> =
        if (sourceLocation != null) listOf(sourceLocation) else emptyList()

    override fun getErrorType(): ErrorClassification = ErrorType.ValidationError

    override fun getExtensions(): Map<String, Any> = mapOf(
        "errorType" to SaGrapQlErrorType.FIELD_VALIDATION_FAILURE,
        "validationErrors" to validationDetails,
    )

    override fun getPath(): List<Any> = queryPath
}
