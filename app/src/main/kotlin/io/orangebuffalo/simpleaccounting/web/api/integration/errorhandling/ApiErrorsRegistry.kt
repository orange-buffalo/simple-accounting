package io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.result.condition.PatternsRequestCondition
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Introspects Spring Web endpoints for [HandleApiErrorsWith] annotation and collects corresponding handlers.
 * Is intended to be used by [SaOpenApiCustomizer] and [RestApiControllerExceptionsHandler].
 */
@Component
internal class ApiErrorsRegistry(
    restApiMappings: List<RequestMappingInfoHandlerMapping>,
) {
    val errorDescriptors: List<ApiErrorDescriptor> = restApiMappings.asSequence()
        .flatMap { it.handlerMethods.entries.asSequence() }
        .flatMap { (requestMappingInfo, handlerMethod) ->
            val handleApiErrorWiths = handlerMethod.method.getAnnotationsByType(HandleApiErrorsWith::class.java)
            val patterns = requestMappingInfo.patternsCondition.patterns.map { it.patternString }
            val methods = requestMappingInfo.methodsCondition.methods.map { it.asHttpMethod() }
            handleApiErrorWiths.map {
                val errorHandlerClass = it.errorHandler
                val errorHandler = errorHandlerClass.primaryConstructor?.call()
                    ?: throw IllegalStateException("$errorHandlerClass (from ${handlerMethod.method}) cannot be instantiated")

                ApiErrorDescriptor(
                    paths = patterns,
                    httpMethods = methods,
                    responseBody = errorHandler.getResponseType(),
                    responseStatus = errorHandler.getHttpStatus(),
                    errorHandler = errorHandler,
                    patternsCondition = requestMappingInfo.patternsCondition,
                )
            }
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
         * Body type for the error response.
         */
        val responseBody: KClass<*>,
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
