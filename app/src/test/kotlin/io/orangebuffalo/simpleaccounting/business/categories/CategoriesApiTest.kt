package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@DisplayName("Categories API")
internal class CategoriesApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val fryWorkspace = workspace(owner = fry)
            val farnsworthWorkspace = workspace(owner = farnsworth)
            val slurmCategory = category(
                name = "Slurm", workspace = fryWorkspace, description = "..", income = false, expense = true
            )
            val planetExpressCategory = category(
                name = "PlanetExpress",
                workspace = fryWorkspace,
                description = "...",
                income = true,
                expense = false
            )
        }
    }

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/categories")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should get categories of current user workspace`() {
        client.get()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/categories")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("name", "PlanetExpress")
                        put("id", preconditions.planetExpressCategory.id)
                        put("version", 0)
                        put("description", "...")
                        put("income", true)
                        put("expense", false)
                    }
                    addJsonObject {
                        put("name", "Slurm")
                        put("id", preconditions.slurmCategory.id)
                        put("version", 0)
                        put("description", "..")
                        put("income", false)
                        put("expense", true)
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should get 404 when requesting categories of another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.farnsworthWorkspace.id}/categories")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${preconditions.farnsworthWorkspace.id} is not found")
            }
    }

    @Test
    fun `should allow POST access only for logged in users`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/categories")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should add a new category to the workspace`() {
        client.post()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}/categories")
            .sendJson(
                """{
                    "name": "1990s stuff",
                    "description": "Stuff from the best time",
                    "income": false,
                    "expense": true
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    name: "1990s stuff",
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0,
                    description: "Stuff from the best time",
                    income: false,
                    expense: true
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should get 404 when adding category to workspace of another user`() {
        client.post()
            .uri("/api/workspaces/${preconditions.farnsworthWorkspace.id}/categories")
            .sendJson(
                """{
                    "name": "1990s stuff",
                    "description": "Stuff from the best time",
                    "income": false,
                    "expense": true
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${preconditions.farnsworthWorkspace.id} is not found")
            }
    }
}
