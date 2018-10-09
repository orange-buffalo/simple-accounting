package io.orangebuffalo.accounting.simpleaccounting.web.api.utils

import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange

//@Component
class ApiPageRequestResolver(
        adapterRegistry: ReactiveAdapterRegistry
) : HandlerMethodArgumentResolverSupport(adapterRegistry), SyncHandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return checkParameterTypeNoReactiveWrapper(parameter) { type ->
            type == ApiPageRequest::class.java
        }
    }

    override fun resolveArgumentValue(
            parameter: MethodParameter,
            bindingContext: BindingContext,
            exchange: ServerWebExchange): Any? {

        val annotation = parameter.annotatedElement.getAnnotation(ApiDto::class.java)
                ?: throw IllegalArgumentException("Missing @ApiDto at ${parameter.method}")

        return ApiPageRequest()
    }
}