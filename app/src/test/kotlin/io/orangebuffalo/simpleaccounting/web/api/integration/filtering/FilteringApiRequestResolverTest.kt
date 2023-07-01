package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
@DisplayName("FilteringApiRequestResolver")
class FilteringApiRequestResolverTest {

    @Mock
    private lateinit var exchange: ServerWebExchange

    private val apiPageRequestResolver = FilteringApiRequestResolver()

    companion object {
        private val defaultRequest = FilteringApiRequest(
            pageNumber = 1,
            pageSize = 10,
            sortBy = null,
            sortDirection = null,
            predicates = emptyList()
        )

        @Suppress("unused")
        @JvmStatic
        fun getUseCases(): Stream<FilteringApiRequestResolverUseCase> = Stream.of(
            FilteringApiRequestResolverUseCase(
                description = "should return default page config if no parameters are specified in the request",
                expectedRequest = defaultRequest
            ),

            FilteringApiRequestResolverUseCase(
                description = "should ignore new API parameters",
                expectedRequest = defaultRequest,
                queryParams = queryParams(
                    "pageNumber" to "20",
                    "pageSize" to "10",
                    "sortOrder" to "10",
                )
            ),

            FilteringApiRequestResolverUseCase(
                description = "should return and error when multiple limit parameters supplied",
                expectedError = "Only a single 'limit' parameter is supported",
                queryParams = queryParams(
                    "limit" to "20",
                    "limit" to "10"
                )
            ),

            FilteringApiRequestResolverUseCase(
                description = "should use provided limit to populate page size",
                expectedRequest = defaultRequest.copy(
                    pageSize = 20
                ),
                queryParams = queryParams("limit" to "20")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should return and error when limit is not a valid int",
                expectedError = "Invalid 'limit' parameter value '20$'",
                queryParams = queryParams("limit" to "20$")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should return and error when multiple page parameters supplied",
                expectedError = "Only a single 'page' parameter is supported",
                queryParams = queryParams(
                    "page" to "1",
                    "page" to "2"
                )
            ),

            FilteringApiRequestResolverUseCase(
                description = "should use provided page to populate page number",
                expectedRequest = defaultRequest.copy(
                    pageNumber = 7
                ),
                queryParams = queryParams("page" to "7")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should return and error when page is not a valid int",
                expectedError = "Invalid 'page' parameter value 'o_O'",
                queryParams = queryParams("page" to "o_O")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should return and error when page is not a valid int",
                expectedError = "Only a single 'sortBy' parameter is supported",
                queryParams = queryParams(
                    "sortBy" to "apiField desc",
                    "sortBy" to "apiField2 desc"
                )
            ),

            FilteringApiRequestResolverUseCase(
                description = "should fail if direction is not provided for sortBy",
                expectedError = "'apiField' is not a valid sorting expression",
                queryParams = queryParams("sortBy" to "apiField")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should fail on invalid sortBy if extra data is provided",
                expectedError = "'apiField desc else' is not a valid sorting expression",
                queryParams = queryParams("sortBy" to "apiField desc else")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should fail on invalid sortBy if direction is not supported",
                expectedError = "'greaterFirst' is not a valid sorting direction",
                queryParams = queryParams("sortBy" to "apiField greaterFirst")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should set desc sorting by query parameter",
                expectedRequest = defaultRequest.copy(
                    sortBy = "apiField",
                    sortDirection = FilteringApiRequestSortDirection.DESC
                ),
                queryParams = queryParams("sortBy" to "apiField desc")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should set asc sorting by query parameter",
                expectedRequest = defaultRequest.copy(
                    sortBy = "apiField",
                    sortDirection = FilteringApiRequestSortDirection.ASC
                ),
                queryParams = queryParams("sortBy" to "apiField asc")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should return a predicate as created by filter if queried filter is supported",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42",
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                queryParams = queryParams("apiField[eq]" to "42")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should fail if filter query is not valid",
                expectedError = "'apiField[eq' is not a valid filter expression",
                queryParams = queryParams("apiField[eq" to "42")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should fail if filter query operator is unknown",
                expectedError = "'op' is not a valid filter operator",
                queryParams = queryParams("apiField[op]" to "42")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should support 'eq' operator",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42",
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                queryParams = queryParams("apiField[eq]" to "42")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should support 'goe' operator",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42",
                            operator = FilteringApiPredicateOperator.GOE
                        )
                    )
                ),
                queryParams = queryParams("apiField[goe]" to "42")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should support 'loe' operator",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42",
                            operator = FilteringApiPredicateOperator.LOE
                        )
                    )
                ),
                queryParams = queryParams("apiField[loe]" to "42")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should support 'in' operator",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42,54",
                            operator = FilteringApiPredicateOperator.IN
                        )
                    )
                ),
                queryParams = queryParams("apiField[in]" to "42,54")
            ),

            FilteringApiRequestResolverUseCase(
                description = "should support multiple filters",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42",
                            operator = FilteringApiPredicateOperator.GOE
                        ),
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "100",
                            operator = FilteringApiPredicateOperator.LOE
                        ),
                        FilteringApiRequestPredicate(
                            apiField = "anotherApiField",
                            value = "abc",
                            operator = FilteringApiPredicateOperator.GOE
                        )
                    )
                ),
                queryParams = queryParams(
                    "apiField[loe]" to "100",
                    "apiField[goe]" to "42",
                    "anotherApiField[goe]" to "abc"
                )
            ),

            FilteringApiRequestResolverUseCase(
                description = "should support operators with multiple values",
                expectedRequest = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "42",
                            operator = FilteringApiPredicateOperator.EQ
                        ),
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "44",
                            operator = FilteringApiPredicateOperator.EQ
                        ),
                        FilteringApiRequestPredicate(
                            apiField = "apiField",
                            value = "20",
                            operator = FilteringApiPredicateOperator.GOE
                        )
                    )
                ),
                queryParams = queryParams(
                    "apiField[eq]" to "42",
                    "apiField[eq]" to "44",
                    "apiField[goe]" to "20"
                )
            )
        )
    }

    @ParameterizedTest
    @MethodSource("getUseCases")
    fun testFilteringApiRequestResolver(useCase: FilteringApiRequestResolverUseCase) {
        val request = Mockito.mock(ServerHttpRequest::class.java)
        whenever(request.queryParams) doReturn useCase.queryParams
        whenever(exchange.request) doReturn request

        when {
            useCase.expectedError != null -> {
                assertThatThrownBy { apiPageRequestResolver.resolveRequest(exchange) }
                    .isInstanceOf(ApiValidationException::class.java)
                    .hasMessage(useCase.expectedError)
            }

            useCase.expectedRequest != null -> {
                val expectedRequest = useCase.expectedRequest
                val resolvedPageRequest = apiPageRequestResolver.resolveRequest(exchange)

                assertThat(resolvedPageRequest).isNotNull
                assertThat(resolvedPageRequest.pageSize).isEqualTo(expectedRequest.pageSize)
                assertThat(resolvedPageRequest.pageNumber).isEqualTo(expectedRequest.pageNumber)
                assertThat(resolvedPageRequest.sortBy).isEqualTo(expectedRequest.sortBy)
                assertThat(resolvedPageRequest.sortDirection).isEqualTo(expectedRequest.sortDirection)
                assertThat(resolvedPageRequest.predicates).isNotNull
                assertThat(resolvedPageRequest.predicates)
                    .containsExactlyInAnyOrder(*expectedRequest.predicates.toTypedArray())
            }

            else -> {
                throw IllegalArgumentException("Invalid use case $useCase")
            }
        }
    }

}

data class FilteringApiRequestResolverUseCase(
    val description: String,
    val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap(),
    val expectedRequest: FilteringApiRequest? = null,
    val expectedError: String? = null
) {
    override fun toString() = description
}

private fun queryParams(vararg params: Pair<String, String>): MultiValueMap<String, String> =
    LinkedMultiValueMap<String, String>()
        .apply {
            params.forEach { add(it.first, it.second) }
        }
