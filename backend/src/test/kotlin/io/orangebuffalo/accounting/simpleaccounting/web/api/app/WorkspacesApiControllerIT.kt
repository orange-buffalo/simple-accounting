package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Farnsworth
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Roberto
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Zoidberg
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Workspaces API ")
internal class WorkspacesApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val workspaceRepo: WorkspaceRepository,
    @Autowired val categoryRepository: CategoryRepository,
    @Autowired val dbHelper: DbHelper
) {

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return workspaces of current user`(fry: Fry) {
        client.get()
            .uri("/api/v1/user/workspaces")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                        name: "Property of Philip J. Fry",
                        id: ${fry.workspace.id},
                        version: 0,
                        taxEnabled: false,
                        multiCurrencyEnabled: false,
                        defaultCurrency: "USD",
                        categories: [{
                            id: ${fry.slurmCategory.id},
                            version: 0,
                            name: "for Slurm",
                            description: "Only for the best drink ever",
                            income: true,
                            expense: true
                        }]
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Zoidberg")
    fun `should return empty list if no workspace exists for user`(zoidberg: Zoidberg) {
        client.get()
            .uri("/api/v1/user/workspaces")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new workspace`(fry: Fry) {
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

        val newWorkspace = workspaceRepo.findById(workspaceId)
        assertThat(newWorkspace).isPresent.hasValueSatisfying {
            assertThat(it.owner).isEqualTo(fry.himself)
        }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should add a new category to the workspace`(fry: Fry) {
        val categoryId = dbHelper.getNextId()

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "name": "1990s stuff",
                    "description": "Stuff from the best time",
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
                        name: "1990s stuff",
                        id: $categoryId,
                        version: 0,
                        description: "Stuff from the best time",
                        income: false,
                        expense: true
                    }"""
                    )
                )
            }

        val newCategory = categoryRepository.findById(categoryId)
        assertThat(newCategory).isPresent.hasValueSatisfying {
            assertThat(it.workspace).isEqualTo(fry.workspace)
        }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 400 if workspace belongs to another user when posting new category`(
        fry: Fry,
        farnsworth: Farnsworth
    ) {
        client.post()
            .uri("/api/v1/user/workspaces/${farnsworth.workspace.id}/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "name": "fry-to-professor",
                    "description": null,
                    "income": false,
                    "expense": true
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${farnsworth.workspace.id} is not found")
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Roberto")
    fun `should calculate expenses statistics`(roberto: Roberto) {
        client.get()
            .uri(
                "/api/v1/user/workspaces/${roberto.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("644")
                inPath("$.finalizedCount").isNumber.isEqualTo("4")
                inPath("$.pendingCount").isNumber.isEqualTo("3")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${roberto.firstCategory.id},
                            "totalAmount": 223,
                            "finalizedCount": 2,
                            "pendingCount": 0
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${roberto.secondCategory.id},
                            "totalAmount": 421,
                            "finalizedCount": 2,
                            "pendingCount": 3
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Roberto")
    fun `should calculate incomes statistics`(roberto: Roberto) {
        client.get()
            .uri(
                "/api/v1/user/workspaces/${roberto.workspace.id}/statistics/incomes" +
                        "?fromDate=3010-04-21&toDate=3010-09-15"
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("568")
                inPath("$.finalizedCount").isNumber.isEqualTo("3")
                inPath("$.pendingCount").isNumber.isEqualTo("2")
                inPath("$.currencyExchangeGain").isNumber.isEqualTo("25")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${roberto.firstCategory.id},
                            "totalAmount": 335,
                            "finalizedCount": 2,
                            "pendingCount": 0,
                            "currencyExchangeGain":25
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${roberto.secondCategory.id},
                            "totalAmount": 233,
                            "finalizedCount": 1,
                            "pendingCount": 2,
                            "currencyExchangeGain": 0
                        }"""
                    )
                )
            }
    }
}