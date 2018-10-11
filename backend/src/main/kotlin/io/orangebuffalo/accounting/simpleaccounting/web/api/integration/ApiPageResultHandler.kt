package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping.ApiDtosMapper
import org.reactivestreams.Publisher
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.core.ResolvableType
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.HandlerResultHandler
import org.springframework.web.reactive.accept.RequestedContentTypeResolver
import org.springframework.web.reactive.result.method.annotation.AbstractMessageWriterResultHandler
import org.springframework.web.server.NotAcceptableStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class ApiPageResultHandler(
        serverCodecConfigurer: ServerCodecConfigurer,
        contentTypeResolver: RequestedContentTypeResolver,
        adapterRegistry: ReactiveAdapterRegistry,
        private val mapper: ApiDtosMapper

) : AbstractMessageWriterResultHandler(
        serverCodecConfigurer.writers,
        contentTypeResolver,
        adapterRegistry), HandlerResultHandler {

    init {
        order = 0
    }

    override fun supports(result: HandlerResult): Boolean {
        val adapter = getAdapter(result)
        return adapter != null
                && !adapter.isNoValue
                && isSupportedType(result.returnTypeSource.nested().nestedParameterType)
    }

    private fun isSupportedType(clazz: Class<*>): Boolean {
        return Page::class.java.isAssignableFrom(clazz)
    }

    override fun handleResult(exchange: ServerWebExchange, result: HandlerResult): Mono<Void> {
        val adapter = getAdapter(result)
                ?: throw IllegalArgumentException("Reactive adapter is missing")

        if (adapter.isMultiValue) {
            throw IllegalArgumentException("Return value should support a single result")
        }

        val bodyParameter = result.returnTypeSource.nested().nested()

        val apiDtoAnnotation = bodyParameter.annotatedElement.getAnnotation(ApiDto::class.java)
                ?: throw IllegalArgumentException("Missing @ApiDto at ${bodyParameter.method}")

        return Mono.from(adapter.toPublisher<Page<Any>>(result.returnValue))
                .flatMap { repositoryPage ->

                    val apiPage = ApiPage(
                            pageNumber = repositoryPage.number + 1,
                            pageSize = repositoryPage.size,
                            totalElements = repositoryPage.totalElements,
                            data = mapper.map(repositoryPage.content, apiDtoAnnotation.dtoClass.java)
                    )

                    val elementType = ResolvableType.forInstance(apiPage)

                    messageWriters.asSequence()
                            .filter { writer ->
                                writer.canWrite(elementType, MediaType.APPLICATION_JSON)
                            }
                            .map { writer ->
                                writer.write(
                                        Mono.just(apiPage) as Publisher<out Nothing>,
                                        elementType,
                                        MediaType.APPLICATION_JSON,
                                        exchange.response,
                                        emptyMap())
                            }
                            .firstOrNull()
                            ?: Mono.defer {
                                Mono.error<Void>(NotAcceptableStatusException("No writer")) }
                }
    }

}