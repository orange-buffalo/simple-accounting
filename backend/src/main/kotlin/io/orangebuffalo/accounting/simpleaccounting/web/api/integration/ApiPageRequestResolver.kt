package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.EntityPath
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

        return validateAndGetSingleParameter("limit", 10, exchange.request.queryParams)
            .map { pageSize -> DataPage(pageSize = pageSize) }
            .flatMap { dataPage ->
                validateAndGetSingleParameter("page", 1, exchange.request.queryParams)
                    .map { pageNumber -> dataPage.copy(pageNumber = pageNumber) }
            }
            .flatMap { dataPage ->
                validateAndGetSort(exchange.request.queryParams, pageableApiDescriptor)
                    .map { sort -> dataPage.copy(sort = sort) }
            }
            .map { dataPage -> ApiPageRequest(page = dataPage.toPageRequest(), predicate = Expressions.TRUE.isTrue) }
            .flatMap { pageRequest ->
                getFiltersPredicate(pageableApiDescriptor, exchange.request.queryParams)
                    .map { predicate ->
                        if (predicate == null) pageRequest else pageRequest.copy(predicate = predicate)
                    }
            }
            .fold({ error -> Mono.error(ApiValidationException(error)) }, { pageRequest -> Mono.just(pageRequest) })
    }

    private fun getFiltersPredicate(
        pageableApiDescriptor: PageableApiDescriptor<*, *>,
        queryParams: MultiValueMap<String, String>
    ): Either<String, Predicate?> {
        val compoundPredicate = BooleanBuilder()
        pageableApiDescriptor.getSupportedFilters().forEach { pageableApiFilter ->
            when (val predicateForFilter = buildPredicatesByFilter(queryParams, pageableApiFilter)) {
                is Either.Left -> return predicateForFilter
                is Either.Right -> if (predicateForFilter.b != null) compoundPredicate.and(predicateForFilter.b)
            }
        }
        return Either.right(compoundPredicate.value)
    }

    private fun buildPredicatesByFilter(
        queryParams: MultiValueMap<String, String>,
        pageableApiFilter: PageableApiFilter<out Any?, out EntityPath<*>>
    ): Either<String, Predicate?> {
        val allOperatorsPredicates = BooleanBuilder()
        queryParams.entries.asSequence()
            .filter { queryParam -> isFilterQueryParam(queryParam.key, pageableApiFilter.apiFieldName) }
            .forEach { queryParam ->
                val queryParamName = queryParam.key
                if (!queryParamName.endsWith("]")) {
                    return Either.left("'${queryParamName}' is not a valid filter expression")
                }

                val rawFilterOperator = queryParamName
                    .removePrefix("${pageableApiFilter.apiFieldName}[")
                    .removeSuffix("]")

                val filterOperator = PageableApiFilterOperator.forApiValue(rawFilterOperator)
                    ?: return Either.left("'${rawFilterOperator}' is not a valid filter operator")

                val rawFilterValues = queryParam.value
                val currentOperatorPredicates = BooleanBuilder()
                rawFilterValues.forEach { rawFilterValue ->
                    when (val maybePredicate = pageableApiFilter.forOperator(filterOperator, rawFilterValue)) {
                        is Either.Left -> return maybePredicate
                        is Either.Right -> currentOperatorPredicates.or(maybePredicate.b)
                    }
                }

                allOperatorsPredicates.and(currentOperatorPredicates.value)
            }

        return Either.right(allOperatorsPredicates.value)
    }

    private fun isFilterQueryParam(queryParam: String, apiFieldName: String) =
        queryParam.startsWith("${apiFieldName}[")

    private fun validateAndGetSort(
        queryParams: MultiValueMap<String, String>,
        pageableApiDescriptor: PageableApiDescriptor<*, *>
    ): Either<String, Sort> {
        val sortByParams = queryParams["sortBy"]
            ?: return Either.right(pageableApiDescriptor.getDefaultSorting())

        if (sortByParams.size > 1) {
            return Either.left("Only a single 'sortBy' parameter is supported")
        }
        val sortBy = sortByParams[0]

        val parts = sortBy.split(" ")
        if (parts.size != 2) {
            return Either.left("'$sortBy' is not a valid sorting expression")
        }

        val directionStr = parts[1].trim()
        val direction = Sort.Direction.fromOptionalString(directionStr)
            .orElse(null)
            ?: return Either.left("'$directionStr' is not a valid sorting direction")

        val apiField = parts[0].trim()
        val entityField = pageableApiDescriptor.getSupportedSorting()[apiField]
            ?: return Either.left("Sorting by '$apiField' is not supported")

        return Either.right(Sort.by(Sort.Order.by(entityField).with(direction)))
    }

    private fun validateAndGetSingleParameter(
        paramName: String,
        defaultValue: Int,
        queryParams: MultiValueMap<String, String>
    ): Either<String, Int> = Either.right(queryParams[paramName])
        .flatMap { param ->
            if (param != null && param.size > 1) {
                Either.left("Only a single '$paramName' parameter is supported")
            } else {
                Either.right(param)
            }
        }
        .flatMap { param ->
            Option.fromNullable(param)
                .map { it[0] }
                .map { paramValue ->
                    try {
                        Either.right(paramValue.toInt())
                    } catch (e: NumberFormatException) {
                        Either.left("Invalid '$paramName' parameter value '$paramValue'")
                    }
                }
                .getOrElse { Either.right(defaultValue) }
        }

    private data class DataPage(
        val pageNumber: Int? = null,
        val pageSize: Int? = null,
        val sort: Sort? = null
    )

    private fun DataPage.toPageRequest() = PageRequest.of(pageNumber!! - 1, pageSize!!, sort!!)
}
