package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class ApiPageRequestResolver(
    adapterRegistry: ReactiveAdapterRegistry,
    private val pageableApiDescriptorResolver: PageableApiDescriptorResolver
) : HandlerMethodArgumentResolverSupport(adapterRegistry) {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return checkParameterTypeNoReactiveWrapper(parameter) { type ->
            type == ApiPageRequest::class.java
        }
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any> {

        val pageableApiDescriptor = pageableApiDescriptorResolver.resolveDescriptor(parameter.annotatedElement)

        return validateAndGetParameter("limit", 10, exchange.request.queryParams)
            .map { PageRequest.of(0, it) }
            .flatMap { pageRequest ->
                validateAndGetParameter("page", 1, exchange.request.queryParams)
                    .map { pageNumber -> PageRequest.of(pageNumber - 1, pageRequest.pageSize) }
            }
            .flatMap { pageRequest ->
                validateAndGetSort(exchange.request.queryParams)
                    .map { sort ->
                        PageRequest.of(
                            pageRequest.pageNumber,
                            pageRequest.pageSize,
                            sort
                        )
                    }
            }
            .map { page -> ApiPageRequest(page = page, predicate = Expressions.TRUE.isTrue) }
            .map { pageRequest ->
                val predicate = getFilterPredicate(pageableApiDescriptor, exchange.request.queryParams)
                if (predicate == null) pageRequest else pageRequest.copy(predicate = predicate)
            }
            .fold({ pageRequest -> Mono.just(pageRequest) }, { error -> Mono.error(error) })
    }

    private fun getFilterPredicate(
        pageableApiDescriptor: PageableApiDescriptor<*, *>,
        queryParams: MultiValueMap<String, String>
    ): Predicate? {
        val compoundPredicate: BooleanBuilder = pageableApiDescriptor.getSupportedFilters()
            .map { filter ->
                queryParams.entries
                    .filter {
                        it.key.startsWith("${filter.apiFieldName}[")
                    }
                    .map {
                        if (!it.key.endsWith("]")) {
                            throw ApiValidationException("'${it.key}' is not a valid filter expression")
                        }

                        Pair(it.key.removePrefix("${filter.apiFieldName}[").removeSuffix("]"), it.value)
                    }
                    .map {
                        Pair(
                            PageableApiFilterOperator.forApiValue(it.first)
                                ?: throw ApiValidationException("'${it.first}' is not a valid filter operator"),
                            it.second
                        )
                    }
                    .map { fieldToValues ->
                        fieldToValues.second
                            .map { value -> filter.forOperator(fieldToValues.first, value) }
                            .fold(BooleanBuilder()) { builder, predicate -> builder.or(predicate) }
                    }
                    .toList()
            }
            .filter { it.isNotEmpty() }
            .fold(BooleanBuilder()) { builder, predicates ->
                builder.also {
                    predicates.forEach { predicate -> builder.and(predicate) }
                }
            }

        return compoundPredicate.value
    }

    private fun validateAndGetSort(queryParams: MultiValueMap<String, String>): Result<Sort, ApiValidationException> {
        return Result.of {
            Sort.by(Sort.Direction.DESC, "id")
        }
    }

    private fun validateAndGetParameter(
        paramName: String,
        defaultValue: Int,
        queryParams: MultiValueMap<String, String>
    ): Result<Int, ApiValidationException> {

        val param = queryParams[paramName]
        if (param != null && param.size > 1) {
            return Result.error(ApiValidationException("Only a single '$paramName' parameter is supported"))
        }

        return Result.of {
            if (param != null) {
                val paramValue = param[0]
                try {
                    paramValue.toInt()
                } catch (e: NumberFormatException) {
                    throw ApiValidationException("Invalid '$paramName' parameter value '$paramValue'")
                }
            } else {
                defaultValue
            }
        }
    }
}