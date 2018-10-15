package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
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
internal class WorkspacesApiControllerIT {

    @Autowired
    lateinit var client: WebTestClient
    
    @Autowired
    lateinit var workspaceRepo: WorkspaceRepository

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return workspaces of current user`() {
        val workspace = workspaceRepo.findAll().first { it.owner.userName == "Fry" }

        client.get()
                .uri("/api/v1/user/workspaces")
                .exchange()
                .expectStatus().isOk
                .expectThatJsonBody {
                    inPath("$").isArray.containsExactly(json("""{
                        name: "w1",
                        id: ${workspace.id},
                        version: 0,
                        taxEnabled: true,
                        multiCurrencyEnabled: false,
                        defaultCurrency: "AUD"
                    }"""))
                }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return empty list if no workspaces exists for user`() {
        client.get()
                .uri("/api/v1/user/workspaces")
                .exchange()
                .expectStatus().isOk
                .expectThatJsonBody {
                    inPath("$").isArray.isEmpty()
                }
    }

    @TestConfiguration
    class Config {
        @Bean
        fun testSetupRunner(
                transactionTemplate: TransactionTemplate,
                entityManager: EntityManager): ApplicationRunner = ApplicationRunner { _ ->

            transactionTemplate.execute {
                val fry = PlatformUser(
                        userName = "Fry",
                        passwordHash = "qwertyHash",
                        isAdmin = false
                )
                entityManager.persist(fry)

                entityManager.persist(Workspace(
                        name = "w1",
                        owner = fry,
                        taxEnabled = true,
                        multiCurrencyEnabled = false,
                        defaultCurrency = "AUD"
                ))

                entityManager.persist(PlatformUser(
                        userName = "Farnsworth",
                        passwordHash = "qwertyHash",
                        isAdmin = false
                ))
            }
        }
    }
}