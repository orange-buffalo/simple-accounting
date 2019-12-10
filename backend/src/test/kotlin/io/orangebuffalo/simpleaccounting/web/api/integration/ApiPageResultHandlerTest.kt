package io.orangebuffalo.simpleaccounting.web.api.integration

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

    @Mock
    private lateinit var pageableApiDescriptorResolver: PageableApiDescriptorResolver

    private lateinit var handler: ApiPageResultHandler

    @BeforeEach
    fun setup() {
        whenever(serverCodecConfigurer.writers) doReturn listOf(httpMessageWriter)

        handler = ApiPageResultHandler(
            serverCodecConfigurer,
            FixedContentTypeResolver(MediaType.APPLICATION_JSON),
            ReactiveAdapterRegistry(),
            pageableApiDescriptorResolver
        )
    }

    @Test
    fun `should not support mono methods`() {
        assertFalse(
            handler.supports(
                HandlerResult(
                    this,
                    Mono.just(Page.empty<Any>()),
                    getMethodParameter("monoMethod")
                )
            )
        )
    }

    @Test
    fun `should not support non-Page methods`() {
        assertFalse(
            handler.supports(
                HandlerResult(
                    this,
                    "",
                    getMethodParameter("nonPageMethod")
                )
            )
        )
    }

    @Test
    fun `should support Page methods`() {
        assertTrue(
            handler.supports(
                HandlerResult(
                    this,
                    Page.empty<Any>(),
                    getMethodParameter("pagerMethod")
                )
            )
        )
    }

    private fun getMethodParameter(methodName: String): MethodParameter {
        return MethodParameter.forExecutable(
            TestController::class.java.getDeclaredMethod(methodName), -1
        )
    }

    private class TestController {

        @GetMapping
        fun nonPageMethod(): String {
            return ""
        }

        @GetMapping
        fun monoMethod(): Mono<Page<*>> {
            return Mono.empty()
        }

        @GetMapping
        fun pagerMethod(): Page<*> {
            return Page.empty<Any>()
        }
    }
}
