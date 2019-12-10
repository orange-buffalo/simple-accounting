package io.orangebuffalo.simpleaccounting.web.api.integration

import com.querydsl.core.types.EntityPath
import kotlinx.coroutines.reactor.mono
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
    private val pageableApiDescriptorResolver: PageableApiDescriptorResolver

) : AbstractMessageWriterResultHandler(
    serverCodecConfigurer.writers,
    contentTypeResolver,
    adapterRegistry
), HandlerResultHandler {

    init {
        order = 0
    }

    override fun supports(result: HandlerResult): Boolean {
        return isSupportedType(result.returnTypeSource.parameterType)
    }

    private fun isSupportedType(clazz: Class<*>): Boolean {
        return Page::class.java.isAssignableFrom(clazz)
    }

    override fun handleResult(exchange: ServerWebExchange, result: HandlerResult): Mono<Void> {
        val adapter = getAdapter(result)
            ?: throw IllegalArgumentException("Reactive adapter is missing")

        require(!adapter.isMultiValue) { "Return value should support a single result" }

        val bodyParameter = result.returnTypeSource.nested().nested()

        @Suppress("UNCHECKED_CAST")
        val pageableApiDescriptor: PageableApiDescriptor<Any, EntityPath<Any>> =
            pageableApiDescriptorResolver.resolveDescriptor(bodyParameter.annotatedElement)
                    as PageableApiDescriptor<Any, EntityPath<Any>>

        return Mono.from(adapter.toPublisher<Page<Any>>(result.returnValue))
            .flatMap { repositoryPage ->
                //todo #110: wrap the whole method into a coroutine mono: subscribers context needs to be propagated to support security
                mono {
                    ApiPage(
                        pageNumber = repositoryPage.number + 1,
                        pageSize = repositoryPage.size,
                        totalElements = repositoryPage.totalElements,
                        data = repositoryPage.content.map { pageableApiDescriptor.mapEntityToDto(it) }
                    )
                }
            }.flatMap { apiPage ->
                val elementType = ResolvableType.forInstance(apiPage)

                messageWriters.asSequence()
                    .filter { writer ->
                        writer.canWrite(elementType, MediaType.APPLICATION_JSON)
                    }
                    .map { writer ->
                        @Suppress("UNCHECKED_CAST")
                        writer.write(
                            Mono.just(apiPage) as Publisher<out Nothing>,
                            elementType,
                            MediaType.APPLICATION_JSON,
                            exchange.response,
                            emptyMap()
                        )
                    }
                    .firstOrNull()
                    ?: Mono.defer {
                        Mono.error<Void>(NotAcceptableStatusException("No writer"))
                    }
            }
    }
}
