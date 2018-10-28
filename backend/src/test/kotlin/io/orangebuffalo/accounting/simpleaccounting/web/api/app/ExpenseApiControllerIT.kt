package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
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
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Expense API ")
internal class ExpenseApiControllerIT {

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var workspaceRepo: WorkspaceRepository

    @Autowired
    lateinit var dbHelper: DbHelper

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var expenseRepository: ExpenseRepository

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new expense`() {
        val expenseId = dbHelper.getNextId()
        val workspace = workspaceRepo.findAll().first { it.name == "fry-workspace" }
        val category = categoryRepository.findAll().first { it.name == "fry-category" }

        client.post()
            .uri("/api/v1/user/workspaces/${workspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                // todo datePaid
                """{
                    "category": ${category.id},
                    "currency": "USD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [],
                    "notes": "coffee",
                    "percentOnBusinessInBps": 10000
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        //todo dates
                        """{
                            category: ${category.id},
                            currency: "USD",
                            originalAmount: 30000,
                            amountInDefaultCurrency: 42000,
                            actualAmountInDefaultCurrency: 41500,
                            attachments: [],
                            notes: "coffee",
                            percentOnBusinessInBps: 10000,
                            id: $expenseId,
                            version: 0
                    }"""
                    )
                )
            }

        val expense = expenseRepository.findById(expenseId)
        assertThat(expense).isPresent.hasValueSatisfying {
            assertThat(it.category).isEqualTo(category)
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

                val category = Category(
                    name = "fry-category",
                    workspace = workspace,
                    income = true,
                    expense = true
                )
                entityManager.persist(category)
            }
        }
    }

}