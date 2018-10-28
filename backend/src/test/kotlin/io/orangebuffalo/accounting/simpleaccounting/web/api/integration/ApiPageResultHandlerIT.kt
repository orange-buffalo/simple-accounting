package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping.ApiDtoMapperAdapter
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private const val PATH = "/api/v1/auth/api-page-request-handler-test"

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

    @RestController
    class ApiPageResultHandlerTestController {

        @GetMapping(PATH)
        @ApiDto(ApiUser::class)
        fun get(apiPageRequest: ApiPageRequest): Mono<Page<RepositoryUser>> {
            return Mono.just(
                PageImpl<RepositoryUser>(
                    listOf(
                        RepositoryUser(
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

    data class ApiUser(
        var name: String,
        var internalId: Long,
        var internalVersion: Int
    )

    data class RepositoryUser(
        var userName: String,
        var id: Long,
        var version: Int,
        var password: String
    )

    @Component
    class TestUserPropertyMap
        : ApiDtoMapperAdapter<RepositoryUser, ApiUser>(RepositoryUser::class.java, ApiUser::class.java) {

        override fun map(source: RepositoryUser): ApiUser = ApiUser(
            name = source.userName,
            internalId = source.id,
            internalVersion = source.version
        )
    }
}




