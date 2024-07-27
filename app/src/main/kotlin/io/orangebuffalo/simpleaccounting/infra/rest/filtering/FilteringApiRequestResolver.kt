package io.orangebuffalo.simpleaccounting.infra.rest.filtering

import arrow.core.getOrElse
import io.orangebuffalo.simpleaccounting.infra.Maybe
import io.orangebuffalo.simpleaccounting.infra.rest.errorhandling.ApiValidationException
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange

enum class FilteringApiRequestSortDirection {
    ASC,
    DESC;

    companion object {
        internal fun fromRequestString(value: String): FilteringApiRequestSortDirection? =
            values().firstOrNull { it.name.lowercase() == value }
    }
}

@Suppress("LeakingThis", "We control initialization of all sealed class implementations")
sealed class FilteringApiPredicateOperator(val requestValue: String) {
    init {
        knownOperators[requestValue] = this
    }

    override fun toString() = requestValue

    sealed class SingleArgumentOperator(requestValue: String) : FilteringApiPredicateOperator(requestValue) {
        object EQ : SingleArgumentOperator("eq")
        object GOE : SingleArgumentOperator("goe")
        object LOE : SingleArgumentOperator("loe")
    }

    sealed class MultiArgumentsOperator(requestValue: String) : FilteringApiPredicateOperator(requestValue) {
        object IN : MultiArgumentsOperator("in")
    }

    companion object {
        private val knownOperators = mutableMapOf<String, FilteringApiPredicateOperator>()

        val EQ = SingleArgumentOperator.EQ
        val GOE = SingleArgumentOperator.GOE
        val LOE = SingleArgumentOperator.LOE
        val IN = MultiArgumentsOperator.IN

        fun fromRequestString(requestValue: String): FilteringApiPredicateOperator? = knownOperators[requestValue]
    }
}

data class FilteringApiRequestPredicate(
    val apiField: String,
    val value: String,
    val operator: FilteringApiPredicateOperator
)

data class FilteringApiRequest(
    /**
     * Page number, 1-based.
     */
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

                val predicateOperator = FilteringApiPredicateOperator.fromRequestString(rawPredicateOperator)
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
        !setOf("sortBy", "limit", "page", "pageNumber", "pageSize", "sortOrder").contains(queryParam)

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
        return Maybe.fromNullable(param)
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
