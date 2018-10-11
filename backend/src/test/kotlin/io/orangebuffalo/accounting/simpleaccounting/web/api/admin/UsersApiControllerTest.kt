package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiDto
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
internal class UsersApiControllerTest {

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `should return a JWT token for valid admin login credentials`() {
//        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true
//
//        whenever(jwtService.buildJwtToken(argThat {
//            username == "Farnsworth"
//                    && password == "qwertyHash"
//                    && authorities.size == 1
//                    && authorities.iterator().next().authority == "ROLE_ADMIN"
//        })) doReturn "jwtTokenForFarnsworth"

        client.get().uri("/api/v1/auth/test")
//                .contentType(APPLICATION_JSON)
//                .syncBody(LoginRequest(
//                        userName = "Farnsworth",
//                        password = "qwerty"
//                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.page").isEqualTo(42)
    }

//    @TestConfiguration
//    class Config {
//
//        @Bean
//        fun testController(): Any {
//            return TestRestController()
//        }
//
//    }

    @RestController
    class TestRestController {

        @GetMapping("/api/v1/auth/test")
        @ApiDto(TestDto::class)
        fun get(apiPageRequest: ApiPageRequest): Page<TestEntity> {
            return PageImpl(listOf(TestEntity(1, "1")), PageRequest.of(42, 20), 42)
        }

    }


    class TestEntity(val id: Int, val name: String)

    class TestDto(val name: String)
}