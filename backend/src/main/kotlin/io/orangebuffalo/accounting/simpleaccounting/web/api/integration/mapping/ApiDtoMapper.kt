package io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping

import org.springframework.stereotype.Service

@Service
class ApiDtosMapper(
        private val mappers: List<ApiDtoMapper<*, *>>
) {

    fun <S : Any, D : Any> map(sources: List<S>, destinationClass: Class<D>): List<D> {
        if (sources.isEmpty()) {
            return emptyList()
        }

        val sourceClass = sources[0].javaClass
        val apiDtoMapper = mappers.asSequence()
                .filter { it.getSourceClass() == sourceClass }
                .filter { it.getDestinationClass() == destinationClass }
                .firstOrNull()
                ?: throw IllegalArgumentException("No mapper for $sourceClass -> $destinationClass")

        return sources.map { (apiDtoMapper as ApiDtoMapper<S, D>).map(it) }
    }
}

interface ApiDtoMapper<S : Any, D : Any> {

    fun map(source: S): D

    fun getSourceClass(): Class<S>

    fun getDestinationClass(): Class<D>

}

abstract class ApiDtoMapperAdapter<S : Any, D : Any>(
        private val _sourceClass: Class<S>,
        private val _destinationClass: Class<D>
) : ApiDtoMapper<S, D> {

    override fun getSourceClass(): Class<S> = _sourceClass

    override fun getDestinationClass(): Class<D> = _destinationClass
}