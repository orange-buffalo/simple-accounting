package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.ErrorClassification
import graphql.ErrorType
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.execution.NonNullableValueCoercedAsNullException
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
 * Message patterns that indicate a null or missing value for a non-nullable field argument or variable.
 * The first capture group is the field/argument name in all patterns.
 *
 * Covers both [ValidationError] messages (inline null literals and absent fields in query document)
 * and [NonNullableValueCoercedAsNullException] messages (null passed via GraphQL variables at coercion time).
 */
private val NULL_FIELD_PATTERNS = listOf(
    // ProvidedNonNullArguments.missingFieldArg: "Validation error (X) : Missing field argument 'fieldName'"
    Regex("""Missing field argument '([^']+)'"""),
    // ProvidedNonNullArguments.nullValue: "Validation error (X) : Null value for non-null field argument 'fieldName'"
    Regex("""Null value for non-null field argument '([^']+)'"""),
    // ArgumentValidationUtil.handleNullError: "Validation error (X) : argument 'fieldName' with value '...' must not be null"
    Regex("""argument '([^']+)' with value .+ must not be null"""),
    // NonNullableValueCoercedAsNullException: "Variable 'varName' has coerced Null value for NonNull type 'Type'"
    // Used when a top-level variable declared as non-null receives a null value.
    Regex("""Variable '([^']+)' has coerced Null value for NonNull type"""),
    // NonNullableValueCoercedAsNullException: "Field 'fieldName' of variable 'varName' has coerced Null value for NonNull type 'Type'"
    // Used when a field inside an input-object variable receives a null value.
    Regex("""Field '([^']+)' of variable '[^']+' has coerced Null value for NonNull type"""),
)

/**
 * Instrumentation that transforms null/missing-field errors for non-nullable arguments or variables
 * into the same [SaGrapQlErrorType.FIELD_VALIDATION_FAILURE] format used by JSR-303 constraint violations.
 *
 * Two sources of null-field errors are handled:
 * - [ValidationError] — produced by graphql-java's document validators when an inline `null` literal
 *   or an absent field is used for a non-null argument in the query document.
 * - [NonNullableValueCoercedAsNullException] — produced by graphql-java's variable coercion when
 *   the frontend passes `null` for a non-null typed variable (e.g. `$rateInBps: Int!`).
 *
 * Both cases are normalized to a consistent [SaGrapQlErrorType.FIELD_VALIDATION_FAILURE] response
 * so API clients can handle validation uniformly regardless of how the null was supplied.
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
            .filter { it is ValidationError || it is NonNullableValueCoercedAsNullException }
            .mapNotNull { error ->
                val message = error.message ?: return@mapNotNull null
                val fieldName = NULL_FIELD_PATTERNS
                    .firstNotNullOfOrNull { pattern -> pattern.find(message)?.groups?.get(1)?.value }
                    ?: return@mapNotNull null
                NullFieldError(
                    originalError = error,
                    fieldName = fieldName,
                    queryPath = when (error) {
                        is ValidationError -> error.queryPath ?: emptyList()
                        else -> error.path?.filterIsInstance<String>() ?: emptyList()
                    },
                )
            }

    private data class NullFieldError(
        val originalError: GraphQLError,
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
