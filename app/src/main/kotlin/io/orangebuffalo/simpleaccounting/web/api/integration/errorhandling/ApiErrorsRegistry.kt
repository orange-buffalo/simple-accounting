package io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.result.condition.PatternsRequestCondition
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping
import kotlin.reflect.full.primaryConstructor

/**
 * Introspects Spring Web endpoints for [HandleApiErrorsWith] and [ApiErrorMapping] annotations
 * and collects corresponding handlers.
 * Is intended to be used by [SaOpenApiCustomizer] for schema generation and by
 * [RestApiControllerExceptionsHandler] for runtime exception handling.
 */
@Component
internal class ApiErrorsRegistry(
    restApiMappings: List<RequestMappingInfoHandlerMapping>,
) {
    val errorDescriptors: List<ApiErrorDescriptor> = restApiMappings.asSequence()
        .flatMap { it.handlerMethods.entries.asSequence() }
        .flatMap { (requestMappingInfo, handlerMethod) ->
            val patterns = requestMappingInfo.patternsCondition.patterns.map { it.patternString }
            val methods = requestMappingInfo.methodsCondition.methods.map { it.asHttpMethod() }

            val handleApiErrorWiths = handlerMethod.method.getAnnotationsByType(HandleApiErrorsWith::class.java)
            val allHandlers = handleApiErrorWiths.map {
                val errorHandlerClass = it.errorHandler
                val errorHandler = errorHandlerClass.primaryConstructor?.call()
                    ?: throw IllegalStateException("$errorHandlerClass (from ${handlerMethod.method}) cannot be instantiated")

                ApiErrorDescriptor(
                    paths = patterns,
                    httpMethods = methods,
                    responseBodyDescriptor = errorHandler.getResponseBodyDescriptor(),
                    responseStatus = errorHandler.getHttpStatus(),
                    errorHandler = errorHandler,
                    patternsCondition = requestMappingInfo.patternsCondition,
                )
            }.toMutableList()

            val apiErrorMappings = handlerMethod.method.getAnnotationsByType(ApiErrorMapping::class.java)
            if (apiErrorMappings.isNotEmpty()) {
                val errorHandler = SimpleApiErrorHandler(apiErrorMappings, handlerMethod.method)
                allHandlers.add(
                    ApiErrorDescriptor(
                        paths = patterns,
                        httpMethods = methods,
                        responseBodyDescriptor = errorHandler.getResponseBodyDescriptor(),
                        responseStatus = errorHandler.getHttpStatus(),
                        errorHandler = errorHandler,
                        patternsCondition = requestMappingInfo.patternsCondition,
                    )
                )
            }

            allHandlers
        }
        .toList()

    data class ApiErrorDescriptor(
        /**
         * Path patterns to which the error handler applies.
         */
        val paths: List<String>,
        /**
         * Http methods to which the error handler applies.
         */
        val httpMethods: List<HttpMethod>,
        /**
         * Http status for the error response.
         */
        val responseStatus: HttpStatus,
        /**
         * Body descriptor for the error response.
         */
        val responseBodyDescriptor: ApiErrorResponseBodyDescriptor,
        /**
         * Handler for the error.
         */
        val errorHandler: ApiErrorHandler<*>,
        /**
         * Path pattern matcher (to allow testing against web exchange).
         */
        val patternsCondition: PatternsRequestCondition,
    )
}
