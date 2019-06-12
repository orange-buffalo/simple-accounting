package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Farnsworth
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
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
                        defaultCurrency: "USD"
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
                        defaultCurrency: "GPB"
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
}