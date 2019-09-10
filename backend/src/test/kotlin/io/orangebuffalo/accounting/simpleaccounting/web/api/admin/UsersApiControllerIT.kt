package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.sendJson
import io.orangebuffalo.accounting.simpleaccounting.verifyOkAndJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Admin User API ")
internal class UsersApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val passwordEncoder: PasswordEncoder,
    @Autowired val entityManager: EntityManager,
    @Autowired val dbHelper: DbHelper
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
        val leelaId = dbHelper.getNextId()
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
                        id: $leelaId,
                        version: 0,
                        admin: false
                    }"""
                    )
                )
            }

        val createdUser = entityManager.find(PlatformUser::class.java, leelaId)
        assertThat(createdUser).isNotNull
        assertThat(createdUser?.isAdmin).isFalse()
        assertThat(createdUser?.userName).isEqualTo("Leela")
        assertThat(createdUser?.passwordHash).isNotNull().matches { passwordEncoder.matches("&#(3", it as String) }
    }
}

class UserApiTestData : TestData {
    val fry = Prototypes.fry()
    val farnsworth = Prototypes.farnsworth()

    override fun generateData() = listOf(farnsworth, fry)
}