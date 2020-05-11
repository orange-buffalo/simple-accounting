package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.sendJson
import io.orangebuffalo.simpleaccounting.verifyOkAndJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Admin User API ")
internal class UsersApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should allow access only for Admin to read users`() {
        client.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should allow access only for Admin to create users`() {
        client.post()
            .uri("/api/users")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return a valid users page`(testData: UserApiTestData) {
        client.get()
            .uri("/api/users")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactly(
                    json(
                        """{
                        userName: "Fry",
                        id: ${testData.fry.id},
                        version: 0,
                        admin: false
                    }"""
                    ),
                    json(
                        """{
                        userName: "Farnsworth",
                        id: ${testData.farnsworth.id},
                        version: 0,
                        admin: true
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should create a new user`(testData: UserApiTestData) {
        client.post()
            .uri("/api/users")
            .sendJson(
                """{
                    "userName": "Leela",
                    "password": "&#(3",
                    "admin": false
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                        userName: "Leela",
                        id: "#{json-unit.any-number}",
                        version: 0,
                        admin: false
                    }"""
                    )
                )
            }
    }
}

class UserApiTestData : TestData {
    val farnsworth = Prototypes.farnsworth()
    val fry = Prototypes.fry()

    override fun generateData() = listOf(farnsworth, fry)
}
