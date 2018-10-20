package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Workspaces API ")
internal class WorkspacesApiControllerIT {

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var workspaceRepo: WorkspaceRepository

    @Autowired
    lateinit var userRepository: PlatformUserRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var dbHelper: DbHelper

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return workspaces of current user`() {
        val workspace = workspaceRepo.findAll().first { it.owner.userName == "Fry" }
        val category = categoryRepository.findAll().first { it.name == "c1" }

        client.get()
            .uri("/api/v1/user/workspaces")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                        name: "fry-workspace",
                        id: ${workspace.id},
                        version: 0,
                        taxEnabled: true,
                        multiCurrencyEnabled: false,
                        defaultCurrency: "AUD",
                        categories: [{
                            id: ${category.id},
                            version: 0,
                            name: "c1",
                            description: "Fry's category",
                            income: true,
                            expense: false
                        }]
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Farnsworth")
    fun `should return empty list if no workspace exists for user`() {
        client.get()
            .uri("/api/v1/user/workspaces")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Leela")
    fun `should create a new workspace`() {
        val workspaceId = dbHelper.getNextId()

        client.post()
            .uri("/api/v1/user/workspaces")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "name": "wp",
                    "taxEnabled": false,
                    "multiCurrencyEnabled": true,
                    "defaultCurrency": "GPB"
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "wp",
                        id: $workspaceId,
                        version: 0,
                        taxEnabled: false,
                        multiCurrencyEnabled: true,
                        defaultCurrency: "GPB",
                        categories: []
                    }"""
                    )
                )
            }

        val leela = userRepository.findByUserName("Leela")
        val newWorkspace = workspaceRepo.findById(workspaceId)
        assertThat(newWorkspace.map(Workspace::owner)).isEqualTo(leela)
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Leela")
    fun `should add a new category to the workspace`() {
        val categoryId = dbHelper.getNextId()
        val workspace = workspaceRepo.findAll().first { it.name == "leela-categories-workspace" }

        client.post()
            .uri("/api/v1/user/workspaces/${workspace.id}/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "name": "leela-new-category",
                    "description": "Description of important category",
                    "income": false,
                    "expense": true
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "leela-new-category",
                        id: $categoryId,
                        version: 0,
                        description: "Description of important category",
                        income: false,
                        expense: true
                    }"""
                    )
                )
            }

        val newCategory = categoryRepository.findById(categoryId)
        assertThat(newCategory.map(Category::workspace))
            .isPresent
            .hasValue(workspace)
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 400 if workspace belongs to another user`() {
        val workspace = workspaceRepo.findAll().first { it.name == "leela-categories-workspace" }

        client.post()
            .uri("/api/v1/user/workspaces/${workspace.id}/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "name": "fry-to-leela",
                    "description": null,
                    "income": false,
                    "expense": true
                }"""
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${workspace.id} cannot be found")
            }
    }

    @TestConfiguration
    class Config {
        @Bean
        fun testSetupRunner(
            transactionTemplate: TransactionTemplate,
            entityManager: EntityManager
        ): ApplicationRunner = ApplicationRunner { _ ->

            transactionTemplate.execute {
                val fry = PlatformUser(
                    userName = "Fry",
                    passwordHash = "qwertyHash",
                    isAdmin = false
                )
                entityManager.persist(fry)

                val workspace = Workspace(
                    name = "fry-workspace",
                    owner = fry,
                    taxEnabled = true,
                    multiCurrencyEnabled = false,
                    defaultCurrency = "AUD"
                )

                entityManager.persist(workspace)

                entityManager.persist(
                    Category(
                        name = "c1",
                        description = "Fry's category",
                        income = true,
                        expense = false,
                        workspace = workspace
                    )
                )

                entityManager.persist(
                    PlatformUser(
                        userName = "Farnsworth",
                        passwordHash = "qwertyHash",
                        isAdmin = false
                    )
                )

                val leela = PlatformUser(
                    userName = "Leela",
                    passwordHash = "qwertyHash",
                    isAdmin = false
                )
                entityManager.persist(leela)

                entityManager.persist(
                    Workspace(
                        name = "leela-categories-workspace",
                        owner = leela,
                        taxEnabled = true,
                        multiCurrencyEnabled = false,
                        defaultCurrency = "AUD"
                    )
                )
            }
        }
    }
}