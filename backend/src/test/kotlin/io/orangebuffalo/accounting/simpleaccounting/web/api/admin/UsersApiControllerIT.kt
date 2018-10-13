package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
internal class UsersApiControllerIT {

    @Autowired
    lateinit var client: WebTestClient

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return a valid users page`() {
        client.get()
                .uri("/api/v1/admin/users")
                .exchange()
                .expectStatus().isOk
                .expectThatJsonBody {
                    inPath("$.pageNumber").isNumber.isEqualTo("1")
                    inPath("$.pageSize").isNumber.isEqualTo("10")
                    inPath("$.totalElements").isNumber.isEqualTo("2")

                    inPath("$.data").isArray.containsExactly(json("""{
                        userName: "Farnsworth",
                        id: 2,
                        version: 0,
                        admin: true
                    }"""), json("""{
                        userName: "Fry",
                        id: 1,
                        version: 0,
                        admin: false
                    }"""))
                }
    }

    @TestConfiguration
    class Config {
        @Bean
        fun testSetupRunner(
                transactionTemplate: TransactionTemplate,
                entityManager: EntityManager): ApplicationRunner = ApplicationRunner { _ ->

            transactionTemplate.execute {
                entityManager.persist(PlatformUser(
                        userName = "Fry",
                        passwordHash = "qwertyHash",
                        isAdmin = false
                ))

                entityManager.persist(PlatformUser(
                        userName = "Farnsworth",
                        passwordHash = "qwertyHash",
                        isAdmin = true
                ))
            }
        }
    }
}