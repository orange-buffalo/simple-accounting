package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.querydsl.core.types.dsl.PathBuilder
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private const val PATH = "/api/auth/api-page-request-handler-test"

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
internal class ApiPageResultHandlerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should map a result as per annotation configuration`() {
        client.get().uri(PATH)
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("3")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("42")

                inPath("$.data").isArray.containsExactly(
                    json(
                        """{
                        name: "Leela",
                        internalId: 1,
                        internalVersion: 0
                    }"""
                    )
                )
            }
    }

    @TestConfiguration()
    class ApiPageResultHandlerTesConfig {
        @Bean
        fun apiPageResultHandlerTestController(): ApiPageResultHandlerTestController =
            ApiPageResultHandlerTestController()

        @Bean
        fun apiPageResultHandlerTestPageableApiDescriptor() = ApiPageResultHandlerTestPageableApiDescriptor()
    }

    @RestController
    class ApiPageResultHandlerTestController {

        @GetMapping(PATH)
        @PageableApi(ApiPageResultHandlerTestPageableApiDescriptor::class)
        fun get(apiPageRequest: ApiPageRequest): Mono<Page<ApiPageResultHandlerTestRepositoryUser>> {
            return Mono.just(
                PageImpl<ApiPageResultHandlerTestRepositoryUser>(
                    listOf(
                        ApiPageResultHandlerTestRepositoryUser(
                            "Leela",
                            1,
                            0,
                            "&#@*(@#(MG;dfd6yrtFdl3'0s*^"
                        )
                    ),
                    PageRequest.of(2, 10),
                    42
                )
            )
        }
    }

    data class ApiPageResultHandlerTestApiUser(
        var name: String,
        var internalId: Long,
        var internalVersion: Int
    )

    data class ApiPageResultHandlerTestRepositoryUser(
        var userName: String,
        var id: Long,
        var version: Int,
        var password: String
    )

    class ApiPageResultHandlerTestPageableApiDescriptor :
        PageableApiDescriptor<ApiPageResultHandlerTestRepositoryUser, PathBuilder<ApiPageResultHandlerTestRepositoryUser>> {
        override suspend fun mapEntityToDto(entity: ApiPageResultHandlerTestRepositoryUser): ApiPageResultHandlerTestApiUser =
            ApiPageResultHandlerTestApiUser(
                name = entity.userName,
                internalId = entity.id,
                internalVersion = entity.version
            )
    }
}
