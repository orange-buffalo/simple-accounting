package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Admin User API ")
internal class UsersApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    @WithMockFryUser
    fun `should allow access only for Admin to read users`() {
        client.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockFryUser
    fun `should allow access only for Admin to create users`() {
        client.post()
            .uri("/api/users")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return a valid users page`(testData: UserApiTestData) {
        client.get()
            .uri("/api/users")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("3")

                inPath("$.data").isArray.containsExactly(
                    json(
                        """{
                        userName: "Farnsworth",
                        id: ${testData.farnsworth.id},
                        version: 0,
                        admin: true,
                        activated: true
                    }"""
                    ),
                    json(
                        """{
                        userName: "Fry",
                        id: ${testData.fry.id},
                        version: 0,
                        admin: false,
                        activated: true
                    }"""
                    ),
                    json(
                        """{
                        userName: "Zoidberg",
                        id: ${testData.zoidberg.id},
                        version: 0,
                        admin: false,
                        activated: false
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
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
                        admin: false,
                        activated: false
                    }"""
                    )
                )
            }
    }
}

class UserApiTestData : TestData {
    val farnsworth = Prototypes.farnsworth()
    val fry = Prototypes.fry()
    val zoidberg = Prototypes.platformUser(
        userName = "Zoidberg",
        isAdmin = false,
        activated = false
    )

    override fun generateData() = listOf(farnsworth, fry, zoidberg)
}
