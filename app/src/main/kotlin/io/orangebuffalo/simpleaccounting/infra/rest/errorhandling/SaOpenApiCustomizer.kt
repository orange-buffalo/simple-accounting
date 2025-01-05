package io.orangebuffalo.simpleaccounting.infra.rest.errorhandling

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

/**
 * Springdoc has a limitation that it only allows to generate error responses for API schema based on
 * [org.springframework.web.bind.annotation.ExceptionHandler], which can be applied on the whole controller or package,
 * but not on a single endpoint. This makes schema messy and inaccurate, as different endpoints within same controller
 * might produce a different set of errors.
 *
 * This extension uses information from [ApiErrorsRegistry] to generate fine-grained
 * error response schema definitions, keeping it always consistent with actual error handling logic.
 */
@Component
internal class SaOpenApiCustomizer(
    private val apiErrorsRegistry: ApiErrorsRegistry,
) : OpenApiCustomizer {
    override fun customise(openApi: OpenAPI) {
        apiErrorsRegistry.errorDescriptors
            // for stable order of elements in the schema
            .sortedBy { it.responseBodyDescriptor.typeName }
            .forEach { errorDescriptor ->
                val responseBodyDescriptor = errorDescriptor.responseBodyDescriptor

                errorDescriptor.paths.forEach { path ->
                    val pathItem = openApi.paths.computeIfAbsent(path) { _ -> PathItem() }

                    errorDescriptor.httpMethods.forEach { httpMethod ->
                        val swaggerMethod = PathItem.HttpMethod.valueOf(httpMethod.name())
                        val operation = pathItem.readOperationsMap()[swaggerMethod] ?: Operation()
                        val content = Content()
                        content["application/json"] = MediaType()
                            .schema(
                                io.swagger.v3.oas.models.media.Schema<Any>()
                                    .`$ref`("#/components/schemas/${responseBodyDescriptor.typeName}")
                            )
                        operation.responses["${errorDescriptor.responseStatus.value()}"] =
                            ApiResponse()
                                .description(errorDescriptor.responseStatus.reasonPhrase)
                                .content(content)
                        pathItem.operation(swaggerMethod, operation)
                    }
                }

                openApi.components.addSchemas(responseBodyDescriptor.typeName, responseBodyDescriptor.schemaProvider())
            }

        genericResponses.forEach {
            openApi.components.addSchemas(
                it.simpleName, ModelConverters.getInstance(true)
                    .resolveAsResolvedSchema(AnnotatedType(it))
                    .schema
            )
        }
    }
}

private val genericResponses = listOf(
    InvalidInputErrorDto.FieldErrorDto::class.java,
    InvalidInputErrorDto::class.java,
    SaApiErrorDto::class.java,
)
