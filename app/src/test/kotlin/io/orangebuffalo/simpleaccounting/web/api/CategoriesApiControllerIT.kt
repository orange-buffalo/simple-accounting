package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.*
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SimpleAccountingIntegrationTest
@DisplayName("Categories API ")
internal class CategoriesApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should allow GET access only for logged in users`(testData: CategoriesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should get categories of current user workspace`(testData: CategoriesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories")
            .verifyOkAndJsonBody {
                inPath("$.data").isArray.containsExactly(
                    json(
                        """{
                        name: "PlanetExpress",
                        id: ${testData.planetExpressCategory.id},
                        version: 0,
                        description: "...",
                        income: true,
                        expense: false
                    }"""
                    ), json(
                        """{
                        name: "Slurm",
                        id: ${testData.slurmCategory.id},
                        version: 0,
                        description: "..",
                        income: false,
                        expense: true
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should get 404 when requesting categories of another user`(testData: CategoriesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.farnsworthWorkspace.id}/categories")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${testData.farnsworthWorkspace.id} is not found")
            }
    }

    @Test
    fun `should allow POST access only for logged in users`(testData: CategoriesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should add a new category to the workspace`(testData: CategoriesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories")
            .sendJson(
                """{
                    "name": "1990s stuff",
                    "description": "Stuff from the best time",
                    "income": false,
                    "expense": true
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "1990s stuff",
                        id: "#{json-unit.any-number}",
                        version: 0,
                        description: "Stuff from the best time",
                        income: false,
                        expense: true
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should get 404 when adding category to workspace of another user`(testData: CategoriesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.farnsworthWorkspace.id}/categories")
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
                assertThat(it.responseBody).contains("Workspace ${testData.farnsworthWorkspace.id} is not found")
            }
    }

    class CategoriesApiTestData : TestData {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val fryWorkspace = Prototypes.workspace(owner = fry)
        val farnsworthWorkspace = Prototypes.workspace(owner = farnsworth)
        val slurmCategory = Prototypes.category(
            name = "Slurm", workspace = fryWorkspace, description = "..", income = false, expense = true
        )
        val planetExpressCategory = Prototypes.category(
            name = "PlanetExpress",
            workspace = fryWorkspace,
            description = "...",
            income = true,
            expense = false
        )

        override fun generateData() = listOf(
            farnsworth, fry, fryWorkspace, farnsworthWorkspace, slurmCategory, planetExpressCategory
        )
    }
}
