package io.orangebuffalo.accounting.simpleaccounting.web.api.utils

import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.data.domain.Page
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.HandlerResultHandler
import org.springframework.web.reactive.accept.RequestedContentTypeResolver
import org.springframework.web.reactive.result.method.annotation.AbstractMessageWriterResultHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class ApiPageResultHandler(
        serverCodecConfigurer: ServerCodecConfigurer,
        contentTypeResolver: RequestedContentTypeResolver,
        adapterRegistry: ReactiveAdapterRegistry

) : AbstractMessageWriterResultHandler(
        serverCodecConfigurer.writers,
        contentTypeResolver,
        adapterRegistry), HandlerResultHandler {

    init {
        order = 0
    }

    override fun supports(result: HandlerResult): Boolean {
        val valueType = resolveReturnValueType(result);
		if (isSupportedType(valueType)) {
			return true;
		}
		val adapter = getAdapter(result);
		return adapter != null && !adapter.isNoValue() &&
				isSupportedType(result.getReturnType().getGeneric().resolve(Object::class.java));
    }

    private fun resolveReturnValueType( result: HandlerResult) : Class<*>? {
		var valueType = result.getReturnType().getRawClass();
		val value = result.getReturnValue();
		if ((valueType == null || valueType.equals(Object::class.java)) && value != null) {
			valueType = value.javaClass
		}
		return valueType;
	}

	private fun isSupportedType( clazz : Class<*>?) : Boolean {
		return clazz != null && Page::class.java.isAssignableFrom(clazz)
	}

    override fun handleResult(exchange: ServerWebExchange, result: HandlerResult): Mono<Void> {
        val body = result.getReturnValue();
		val bodyTypeParameter = result.getReturnTypeSource();

		return writeBody(ApiPage(77), bodyTypeParameter, exchange);
    }

}