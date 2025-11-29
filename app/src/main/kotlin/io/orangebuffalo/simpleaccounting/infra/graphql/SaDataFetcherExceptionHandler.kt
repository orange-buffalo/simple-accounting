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
import kotlin.reflect.KClass

private val log = KotlinLogging.logger { }

private val mappingsByAnnotationClass: Map<KClass<out Annotation>, ValidationDirectiveMapping> =
    validationDirectiveMappings.associateBy { it.annotationClass }

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

    override fun getExtensions(): Map<String, Any> = mapOf("errorType" to exception.errorType)

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
        val annotationClass = violation.constraintDescriptor.annotation.annotationClass
        val mapping = mappingsByAnnotationClass[annotationClass]
            ?: throw IllegalStateException("No mapping found for validation annotation ${annotationClass.simpleName}")
        
        // Extract just the field name from the property path (skip method name)
        val fieldPath = violation.propertyPath.drop(1).joinToString(".") { it.name }
        
        val error = mutableMapOf<String, Any>(
            "path" to fieldPath,
            "error" to mapping.errorCode,
            "message" to violation.message
        )
        
        // Add constraint parameters if available
        val params = mapping.paramsExtractor?.invoke(violation)
        if (params != null && params.isNotEmpty()) {
            error["params"] = params
        }
        
        return error
    }
}
