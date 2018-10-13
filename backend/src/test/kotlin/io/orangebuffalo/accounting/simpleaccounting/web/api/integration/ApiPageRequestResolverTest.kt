package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApiPageRequestResolver")
internal class ApiPageRequestResolverTest {

    @Mock
    private lateinit var bindingContext: BindingContext

    @Mock
    private lateinit var exchange: ServerWebExchange

    private lateinit var apiPageRequestResolver: ApiPageRequestResolver

    private lateinit var queryParams: MultiValueMap<String, String>

    @BeforeEach
    fun setup() {
        apiPageRequestResolver = ApiPageRequestResolver(ReactiveAdapterRegistry())

        queryParams = LinkedMultiValueMap<String, String>().apply {
            val request = Mockito.mock(ServerHttpRequest::class.java)
            whenever(request.queryParams) doReturn this
            whenever(exchange.request) doReturn request
        }
    }

    @Test
    fun `should not support non-page-request parameters`() {
        assertFalse(apiPageRequestResolver.supportsParameter(getFirstMethodParameter("inapplicableMethod")))
    }

    @Test
    fun `should support page-request parameters`() {
        assertTrue(apiPageRequestResolver.supportsParameter(getFirstMethodParameter("apiPageMethod")))
    }

    @Test
    fun `should fail on method without ApiDto annotation`() {
        val actualException = assertThrows<IllegalArgumentException> {
            apiPageRequestResolver.resolveArgument(
                    getFirstMethodParameter("apiPageMethodWithoutAnnotation"),
                    bindingContext,
                    exchange)
        }
        assertThat(actualException.message).startsWith("Missing @ApiDto at")
    }

    @Test
    fun `should return default page config if no parameters are specified in the request`() {
        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest()

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.pageNumber).isEqualTo(0)
        assertThat(resolvedPageRequest.page.pageSize).isEqualTo(10)
    }

    @Test
    fun `should return and error when multiple limit parameters supplied`() {
        queryParams.apply {
            add("limit", "20")
            add("limit", "10")
        }

        invokeResolveArgumentAndAssertValidationError("Only a single 'limit' parameter is supported")
    }

    @Test
    fun `should use provided limit to populate page size`() {
        queryParams.add("limit", "20")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest()

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.pageNumber).isEqualTo(0)
        assertThat(resolvedPageRequest.page.pageSize).isEqualTo(20)
    }

    @Test
    fun `should return and error when limit is not a valid int`() {
        queryParams.add("limit", "20$")

        invokeResolveArgumentAndAssertValidationError("Invalid 'limit' parameter value '20$'")
    }

    @Test
    fun `should return and error when multiple page parameters supplied`() {
        queryParams.apply {
            add("page", "1")
            add("page", "2")
        }

        invokeResolveArgumentAndAssertValidationError("Only a single 'page' parameter is supported")
    }

    @Test
    fun `should use provided page to populate page number`() {
        queryParams.add("page", "7")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest()

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.pageNumber).isEqualTo(7)
        assertThat(resolvedPageRequest.page.pageSize).isEqualTo(10)
    }

    @Test
    fun `should return and error when page is not a valid int`() {
        queryParams.add("page", "o_O")

        invokeResolveArgumentAndAssertValidationError("Invalid 'page' parameter value 'o_O'")
    }

    private fun invokeResolveArgumentAndAssertValidationError(expectedMessage: String) {
        val resolvedArgument = apiPageRequestResolver.resolveArgument(
                getFirstMethodParameter("apiPageMethod"),
                bindingContext,
                exchange)

        assertThat(resolvedArgument).isNotNull

        val actualException = assertThrows<ApiValidationException> {
            resolvedArgument.block()
        }
        assertThat(actualException.message).isEqualTo(expectedMessage)
    }

    private fun invokeResolveArgumentAndGetPageRequest(): ApiPageRequest {
        val resolvedArgument = apiPageRequestResolver.resolveArgument(
                getFirstMethodParameter("apiPageMethod"),
                bindingContext,
                exchange)

        assertThat(resolvedArgument).isNotNull

        val resolvedMonoValue = resolvedArgument.block()
        assertThat(resolvedMonoValue).isNotNull
        assertThat(resolvedMonoValue).isInstanceOf(ApiPageRequest::class.java)

        return resolvedMonoValue as ApiPageRequest
    }

    private fun getFirstMethodParameter(methodName: String): MethodParameter {
        return MethodParameter.forExecutable(
                TestController::class.java.declaredMethods.first { it.name == methodName }, 0)
    }

    private class TestController {

        @GetMapping
        fun apiPageMethodWithoutAnnotation(request: ApiPageRequest): Mono<Any> {
            return Mono.empty()
        }

        @GetMapping
        fun inapplicableMethod(body: String): Mono<Any> {
            return Mono.empty()
        }

        @GetMapping
        @ApiDto(ApiTestDto::class)
        fun apiPageMethod(request: ApiPageRequest): Mono<TestRepositoryEntity> {
            return Mono.empty()
        }
    }

    private class ApiTestDto
    private class TestRepositoryEntity
}