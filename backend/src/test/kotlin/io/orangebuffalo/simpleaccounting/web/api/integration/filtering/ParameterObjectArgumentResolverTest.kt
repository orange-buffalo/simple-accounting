package io.orangebuffalo.simpleaccounting.web.api.integration.filtering

import io.swagger.v3.oas.annotations.Parameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springdoc.api.annotations.ParameterObject
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.core.MethodParameter
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.BindingContext

class ParameterObjectArgumentResolverTest {

    private val resolver = ParameterObjectArgumentResolver(ApplicationConversionService())

    @Suppress("unused", "UNUSED_PARAMETER")
    fun subjectMethod(
        @ParameterObject data: CustomPageRequest,
        someParam: String
    ) {
    }

    @Test
    fun `should not support parameters not annotated with @ParameterObject`() {
        assertThat(resolver.supportsParameter(getMethodParameter(index = 1))).isFalse
    }

    @Test
    fun `should support parameters annotated with @ParameterObject`() {
        assertThat(resolver.supportsParameter(getMethodParameter())).isTrue
    }

    @Test
    fun `should resolve query parameters to the parameter object`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/path?pageNumber=4&sortBy=NAME&search[eq]=hello&value[in]=1&value[in]=2")
        )

        val resolvedValue = resolver.resolveArgumentValue(getMethodParameter(), BindingContext(), exchange)
        assertThat(resolvedValue).isNotNull.isInstanceOfSatisfying(CustomPageRequest::class.java) { request ->
            assertThat(request.pageNumber).isEqualTo(4)
            assertThat(request.pageSize).isNull()
            assertThat(request.sortOrder).isNull()
            assertThat(request.sortBy).isEqualTo(CustomSortFields.NAME)
            assertThat(request.search).isEqualTo("hello")
            assertThat(request.valueIn).containsExactlyInAnyOrder(1, 2)
        }

    }

    private fun getMethodParameter(index: Int = 0): MethodParameter {
        val method = javaClass.declaredMethods.find { it.name == "subjectMethod" } ?: throw IllegalArgumentException()
        return MethodParameter(method, index)
    }

    class CustomPageRequest : ApiPageRequest<CustomSortFields>() {
        override var sortBy: CustomSortFields? = null

        @field:Parameter(name = "search[eq]")
        var search: String? = null

        @field:Parameter(name = "value[in]")
        var valueIn: List<Int>? = null
    }

    enum class CustomSortFields {
        NAME
    }
}
