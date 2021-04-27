package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.swagger.v3.oas.annotations.Parameter
import org.springdoc.api.annotations.ParameterObject
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.core.MethodParameter
import org.springframework.core.ResolvableType
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import java.lang.reflect.Field

@Component
class ParameterObjectArgumentResolver(
    val conversionService: ConversionService
) : SyncHandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(ParameterObject::class.java)
    }

    override fun resolveArgumentValue(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Any? {
        val objectType = ResolvableType.forMethodParameter(parameter)
        val resolvedType = objectType.resolve() ?: throw IllegalArgumentException("Cannot resolve type for $objectType")
        val resolvedObject = resolvedType.newInstance()

        // Swagger annotations are on the fields
        val fieldsByName = mutableMapOf<String, Field>()
        ReflectionUtils.doWithFields(resolvedType) { field ->
            fieldsByName[field.name] = field
        }

        val propertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(resolvedObject)
        propertyAccessor.propertyDescriptors.forEach { propertyDescriptor ->
            val fieldName = propertyDescriptor.name
            val field = fieldsByName[fieldName]
            if (field != null) {
                val apiParameter = field.getAnnotation(Parameter::class.java)
                val queryParameterName = apiParameter?.name ?: fieldName
                val requestValues = exchange.request.queryParams[queryParameterName]
                if (requestValues != null) {
                    propertyDescriptor.writeMethod.invoke(
                        resolvedObject, conversionService.convert(
                            requestValues,
                            TypeDescriptor(ResolvableType.forInstance(requestValues), null, null),
                            TypeDescriptor(field)
                        )
                    )
                }
            }
        }

        return resolvedObject
    }
}
