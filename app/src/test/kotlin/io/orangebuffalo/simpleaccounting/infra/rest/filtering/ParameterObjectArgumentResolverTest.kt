package io.orangebuffalo.simpleaccounting.infra.rest.filtering

import io.swagger.v3.oas.annotations.Parameter
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springdoc.core.annotations.ParameterObject
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
        resolver.supportsParameter(getMethodParameter(index = 1)).shouldBeFalse()
    }

    @Test
    fun `should support parameters annotated with @ParameterObject`() {
        resolver.supportsParameter(getMethodParameter()).shouldBeTrue()
    }

    @Test
    fun `should resolve query parameters to the parameter object`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/path?pageNumber=4&sortBy=NAME&search[eq]=hello&value[in]=1&value[in]=2")
        )

        val resolvedValue = resolver.resolveArgumentValue(getMethodParameter(), BindingContext(), exchange)
        val request = resolvedValue.shouldNotBeNull().shouldBeInstanceOf<CustomPageRequest>()
        request.pageNumber.shouldBe(4)
        request.pageSize.shouldBeNull()
        request.sortOrder.shouldBeNull()
        request.sortBy.shouldBe(CustomSortFields.NAME)
        request.search.shouldBe("hello")
        request.valueIn.shouldContainExactlyInAnyOrder(1, 2)

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
