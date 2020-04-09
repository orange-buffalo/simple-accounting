package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import arrow.core.*
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiValidationException
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange

enum class FilteringApiRequestSortDirection {
    ASC,
    DESC;

    companion object {
        internal fun fromRequestString(value: String): FilteringApiRequestSortDirection? =
            values().firstOrNull { it.name.toLowerCase() == value }
    }
}

enum class FilteringApiRequestPredicateOperator {
    EQ,
    GOE,
    LOE;

    companion object {
        internal fun fromRequestString(value: String): FilteringApiRequestPredicateOperator? =
            values().firstOrNull { it.name.toLowerCase() == value }
    }
}

data class FilteringApiRequestPredicate(
    val apiField: String,
    val value: String,
    val operator: FilteringApiRequestPredicateOperator
)

data class FilteringApiRequest(
    val pageNumber: Int,
    val pageSize: Int,
    val sortBy: String?,
    val sortDirection: FilteringApiRequestSortDirection?,
    val predicates: Collection<FilteringApiRequestPredicate>
)

@Component
class FilteringApiRequestResolver {
    private val predicateParamRegex = """([a-zA-Z]+)\[([a-z]+)]""".toRegex()

    fun resolveRequest(exchange: ServerWebExchange): FilteringApiRequest =
        validateAndExtractSort(
            exchange.request.queryParams,
            validateAndExtractPredicates(
                exchange.request.queryParams,
                FilteringApiRequest(
                    pageNumber = validateAndGetSingleParameter("page", 1, exchange.request.queryParams),
                    pageSize = validateAndGetSingleParameter("limit", 10, exchange.request.queryParams),
                    predicates = emptyList(),
                    sortDirection = null,
                    sortBy = null
                )
            )
        )

    private fun validateAndExtractPredicates(
        queryParams: MultiValueMap<String, String>,
        request: FilteringApiRequest
    ): FilteringApiRequest = request.copy(
        predicates = queryParams.entries.asSequence()
            .filter { queryParam -> isFilterQueryParam(queryParam.key) }
            .flatMap { queryParam ->
                val queryParamKey = queryParam.key
                val queryParamKeyMatcher = predicateParamRegex.matchEntire(queryParamKey)
                    ?: throw ApiValidationException("'${queryParamKey}' is not a valid filter expression")

                val rawPredicateOperator = queryParamKeyMatcher.groupValues[2]

                val predicateOperator = FilteringApiRequestPredicateOperator.fromRequestString(rawPredicateOperator)
                    ?: throw ApiValidationException("'${rawPredicateOperator}' is not a valid filter operator")

                val rawPredicateValues = queryParam.value
                val apiField = queryParamKeyMatcher.groupValues[1]
                rawPredicateValues.asSequence()
                    .map { predicateValue ->
                        FilteringApiRequestPredicate(
                            apiField = apiField,
                            operator = predicateOperator,
                            value = predicateValue
                        )
                    }
            }
            .toList())

    private fun isFilterQueryParam(queryParam: String) =
        queryParam != "sortBy" && queryParam != "limit" && queryParam != "page"

    private fun validateAndExtractSort(
        queryParams: MultiValueMap<String, String>,
        request: FilteringApiRequest
    ): FilteringApiRequest {
        val sortByParams = queryParams["sortBy"] ?: return request

        if (sortByParams.size > 1) {
            throw ApiValidationException("Only a single 'sortBy' parameter is supported")
        }
        val sortBy = sortByParams[0]

        val parts = sortBy.split(" ")
        if (parts.size != 2) {
            throw ApiValidationException("'$sortBy' is not a valid sorting expression")
        }

        val directionStr = parts[1].trim()
        val direction = FilteringApiRequestSortDirection.fromRequestString(directionStr)
            ?: throw ApiValidationException("'$directionStr' is not a valid sorting direction")

        val apiField = parts[0].trim()

        return request.copy(
            sortBy = apiField,
            sortDirection = direction
        )
    }

    private fun validateAndGetSingleParameter(
        paramName: String,
        defaultValue: Int,
        queryParams: MultiValueMap<String, String>
    ): Int {
        val param = queryParams[paramName]
        if (param != null && param.size > 1) {
            throw ApiValidationException("Only a single '$paramName' parameter is supported")
        }
        return Option.fromNullable(param)
            .map { it[0] }
            .map { paramValue ->
                try {
                    paramValue.toInt()
                } catch (e: NumberFormatException) {
                    throw ApiValidationException("Invalid '$paramName' parameter value '$paramValue'")
                }
            }
            .getOrElse { defaultValue }
    }
}
