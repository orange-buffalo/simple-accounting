package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.ErrorClassification
import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.language.SourceLocation
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

enum class SaGrapQlErrorType {
    NOT_AUTHORIZED,
}

open class SaGrapQlException(
    message: String,
    val errorType: SaGrapQlErrorType,
    val errorClassification: ErrorClassification = ErrorType.ValidationError,
) : RuntimeException(message)

private class SaGrapQlError(
    val exception: SaGrapQlException,
    private val handlerParameters: DataFetcherExceptionHandlerParameters,
) : GraphQLError {
    override fun getMessage(): String = exception.message!!

    override fun getLocations(): List<SourceLocation> = listOf(handlerParameters.sourceLocation)

    override fun getErrorType(): ErrorClassification = exception.errorClassification

    override fun getExtensions(): Map<String, Any>? = mapOf(
        "errorType" to exception.errorType,
    )

    override fun getPath(): List<Any> = handlerParameters.path.toList()
}
