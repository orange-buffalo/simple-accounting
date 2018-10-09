package io.orangebuffalo.accounting.simpleaccounting.web.api.utils

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.accept.FixedContentTypeResolver
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
internal class ApiPageResultHandlerTest {

    @Mock
    private lateinit var serverCodecConfigurer: ServerCodecConfigurer

    @Mock
    private lateinit var httpMessageWriter: HttpMessageWriter<Any>

    private lateinit var handler: ApiPageResultHandler


    @BeforeEach
    fun setup() {
        whenever(serverCodecConfigurer.writers) doReturn listOf(httpMessageWriter)

        handler = ApiPageResultHandler(
                serverCodecConfigurer,
                FixedContentTypeResolver(MediaType.APPLICATION_JSON),
                ReactiveAdapterRegistry()
        )
    }

    @Test
    fun `should not support non-mono methods`() {
        assertFalse(
                handler.supports(HandlerResult(this, Mono.empty<Any>(), getMethodParameter("nonMonoControllerMethod")))
        )
    }

    @Test
    fun `should not support mono of non-Page methods`() {
        assertFalse(
                handler.supports(HandlerResult(this, Mono.empty<Any>(), getMethodParameter("nonPageMonoControllerMethod")))
        )
    }

    @Test
    fun `should support mono of Page methods`() {
        assertTrue(
                handler.supports(HandlerResult(this, Mono.empty<Any>(), getMethodParameter("emptyMonoControllerMethodWithoutAnnotation")))
        )
    }


    private fun getMethodParameter(methodName: String): MethodParameter {
        return MethodParameter.forExecutable(
                TestController::class.java.getDeclaredMethod(methodName), -1)
    }

    private class TestController {

        @GetMapping
        fun nonPageMonoControllerMethod(): Mono<Any> {
            return Mono.empty()
        }

        @GetMapping
        fun emptyMonoControllerMethodWithoutAnnotation(): Mono<Page<*>> {
            return Mono.empty()
        }

        @GetMapping
        fun nonMonoControllerMethod(): Page<*> {
            return Page.empty<Any>()
        }
    }
}




