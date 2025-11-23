package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.ErrorClassification
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.language.SourceLocation
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlErrorType
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlException
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

private val log = KotlinLogging.logger { }

@Component
class SaDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun handleException(handlerParameters: DataFetcherExceptionHandlerParameters): CompletableFuture<DataFetcherExceptionHandlerResult> {
        return CompletableFuture.completedFuture(mapToResult(handlerParameters))
    }

    private fun mapToResult(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
        val exception = unwrap(handlerParameters.exception)
        
        // Handle Bean Validation exceptions
        if (exception is ConstraintViolationException) {
            return DataFetcherExceptionHandlerResult.newResult()
                .error(ValidationErrorGraphQLError(exception, handlerParameters))
                .build()
        }
        
        if (exception is SaGrapQlException) {
            return DataFetcherExceptionHandlerResult.newResult()
                .error(SaGrapQlError(exception, handlerParameters))
                .build()
        }
        
        log.error(handlerParameters.exception) { "Unexpected exception happened during GraphQL execution" }
        val sourceLocation = handlerParameters.sourceLocation
        val path = handlerParameters.path
        val error = ExceptionWhileDataFetching(path, exception, sourceLocation)
        return DataFetcherExceptionHandlerResult.newResult().error(error).build()
    }

    private fun unwrap(exception: Throwable): Throwable {
        if (exception.cause != null) {
            if (exception is CompletionException) {
                return exception.cause!!
            }
        }
        return exception
    }
}

private class SaGrapQlError(
    val exception: SaGrapQlException,
    private val handlerParameters: DataFetcherExceptionHandlerParameters,
) : GraphQLError {
    override fun getMessage(): String = exception.message!!

    override fun getLocations(): List<SourceLocation> = listOf(handlerParameters.sourceLocation)

    override fun getErrorType(): ErrorClassification = exception.errorClassification

    override fun getExtensions(): Map<String, Any>? {
        return mutableMapOf<String, Any>(
            "errorType" to exception.errorType
        )
    }

    override fun getPath(): List<Any> = handlerParameters.path.toList()
}

/**
 * GraphQL error for Bean Validation (Jakarta) constraint violations.
 * Transforms ConstraintViolationException into a structured validation error response.
 */
private class ValidationErrorGraphQLError(
    private val exception: ConstraintViolationException,
    private val handlerParameters: DataFetcherExceptionHandlerParameters,
) : GraphQLError {
    override fun getMessage(): String = "Validation failed"

    override fun getLocations(): List<SourceLocation> = listOf(handlerParameters.sourceLocation)

    override fun getErrorType(): ErrorClassification = graphql.ErrorType.ValidationError

    override fun getExtensions(): Map<String, Any> {
        val validationErrors = exception.constraintViolations.map { violation ->
            buildValidationError(violation)
        }
        
        return mapOf(
            "errorType" to SaGrapQlErrorType.FIELD_VALIDATION_FAILURE,
            "validationErrors" to validationErrors
        )
    }

    override fun getPath(): List<Any> = handlerParameters.path.toList()
    
    private fun buildValidationError(violation: ConstraintViolation<*>): Map<String, Any> {
        val fieldName = extractFieldName(violation)
        val constraintAnnotation = violation.constraintDescriptor.annotation.annotationClass.simpleName ?: "Unknown"
        val mapping = validationErrorMappings[constraintAnnotation]
        
        val error = mutableMapOf<String, Any>(
            "field" to fieldName,
            "error" to (mapping?.error ?: constraintAnnotation),
            "message" to violation.message
        )
        
        // Add constraint parameters if available
        val params = mapping?.paramsExtractor?.invoke(violation)
        if (params != null && params.isNotEmpty()) {
            error["params"] = params
        }
        
        return error
    }
    
    private fun extractFieldName(violation: ConstraintViolation<*>): String {
        val path = violation.propertyPath.toString()
        // Path format is typically "methodName.parameterName" for method parameters
        // We want just the parameter name
        return path.substringAfterLast('.')
    }
}

private data class ValidationErrorMapping(
    val error: String,
    val paramsExtractor: ((ConstraintViolation<*>) -> Map<String, String>)? = null
)

private val validationErrorMappings = mapOf(
    "NotBlank" to ValidationErrorMapping("MustNotBeBlank"),
    "Size" to ValidationErrorMapping("SizeConstraintViolated") { violation ->
        val attributes = violation.constraintDescriptor.attributes
        mapOf(
            "min" to (attributes["min"]?.toString() ?: "0"),
            "max" to (attributes["max"]?.toString() ?: "2147483647")
        )
    }
)
