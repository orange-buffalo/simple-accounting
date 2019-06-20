package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import io.orangebuffalo.accounting.simpleaccounting.web.verifyUnauthorized
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
    @Autowired val dbHelper: DbHelper
) {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return workspaces of current user`(testData: WorkspacesApiTestData) {
        client.get()
            .uri("/api/workspaces")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                        name: "Property of Philip J. Fry",
                        id: ${testData.fryWorkspace.id},
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
    fun `should return empty list if no workspace exists for user`(testData: WorkspacesApiTestData) {
        client.get()
            .uri("/api/workspaces")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    fun `should allow POST access only for logged in users`() {
        client.post()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new workspace`(testData: WorkspacesApiTestData) {
        val workspaceId = dbHelper.getNextId()

        client.post()
            .uri("/api/workspaces")
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
            assertThat(it.owner).isEqualTo(testData.fry)
        }
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should update a workspace`(testData: WorkspacesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "id": ${testData.farnsworthWorkspace.id},
                    "name": "wp",
                    "taxEnabled": true,
                    "multiCurrencyEnabled": true,
                    "defaultCurrency": "AUD"
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "wp",
                        id: ${testData.fryWorkspace.id},
                        version: 1,
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
    fun `should return 404 on PUT if workspace belongs to another user`(
        testData: WorkspacesApiTestData
    ) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "id": ${testData.fryWorkspace.id},
                    "name": "wp",
                    "taxEnabled": true,
                    "multiCurrencyEnabled": true,
                    "defaultCurrency": "AUD"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${testData.fryWorkspace.id} is not found")
            }
    }

    class WorkspacesApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val zoidberg = Prototypes.zoidberg()

        val fryWorkspace = Workspace(
            name = "Property of Philip J. Fry",
            owner = fry,
            taxEnabled = false,
            multiCurrencyEnabled = false,
            defaultCurrency = "USD"
        )

        val farnsworthWorkspace = Workspace(
            name = "Laboratory",
            owner = farnsworth,
            taxEnabled = false,
            multiCurrencyEnabled = false,
            defaultCurrency = "USD"
        )

        override fun generateData() = listOf(farnsworth, fry, fryWorkspace, farnsworthWorkspace, zoidberg)
    }
}