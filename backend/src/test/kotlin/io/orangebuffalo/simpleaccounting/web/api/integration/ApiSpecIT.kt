package io.orangebuffalo.simpleaccounting.web.api.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.flipkart.zjsonpatch.JsonDiff
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.web.ui.SpaWebFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain

@SimpleAccountingIntegrationTest
@TestPropertySource(
    properties = [
        "springdoc.api-docs.path=/api-docs",
        "springdoc.default-produces-media-type=application/json"
    ]
)
@DisplayName("API Spec")
class ApiSpecIT(
    @Autowired val client: WebTestClient,
    @Autowired @Value("classpath:/api-spec.yaml") val committedSpec: Resource
) {

    @MockBean
    private lateinit var spaWebFilter: SpaWebFilter

    @Test
    fun `should be up-to-date in VCS`() {
        // disable SPA - otherwise docs are not rendered
        whenever(spaWebFilter.filter(any(), any())) doAnswer {
            val webFilterChain = it.arguments[1] as WebFilterChain
            val serverWebExchange = it.arguments[0] as ServerWebExchange
            webFilterChain.filter(serverWebExchange)
        }

        client.get()
            .uri("/api-docs.yaml")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith {
                val currentApiSpec = it.responseBody!!

                val mapper = ObjectMapper(YAMLFactory())
                val schemaDiff = JsonDiff.asJson(
                    mapper.readTree(committedSpec.inputStream),
                    mapper.readTree(currentApiSpec)
                )

                assertThat(schemaDiff).isInstanceOfSatisfying(ArrayNode::class.java) { changes ->
                    assertThat(changes.size())
                        .withFailMessage { schemaDiff.toPrettyString() }
                        .isZero()
                }
            }
    }
}
