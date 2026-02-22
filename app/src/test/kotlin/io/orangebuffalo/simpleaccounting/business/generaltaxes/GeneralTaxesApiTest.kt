package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

internal class GeneralTaxesApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return taxes of a workspace of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("title", "second space tax")
                        put("id", preconditions.secondSpaceTax.id)
                        put("version", 0)
                        put("rateInBps", 3)
                        put("description", "second tax description")
                    }
                    addJsonObject {
                        put("title", "first space tax")
                        put("id", preconditions.firstSpaceTax.id)
                        put("version", 0)
                        put("rateInBps", 4503)
                    }
                }
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        client.get()
            .uri("/api/workspaces/27347947239/general-taxes")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for tax only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/expenses/${preconditions.firstSpaceTax.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return tax by id for current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes/${preconditions.firstSpaceTax.id}")
            .verifyOkAndJsonBody(
                """{
                    title: "first space tax",
                    id: ${preconditions.firstSpaceTax.id},
                    rateInBps: 4503,
                    version: 0
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting tax by id`() {
        client.get()
            .uri("/api/workspaces/5634632/general-taxes/${preconditions.firstSpaceTax.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting tax by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes/${preconditions.firstSpaceTax.id}")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if tax belongs to another workspace when requesting tax by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes/${preconditions.pizzaTax.id}")
            .verifyNotFound("Tax ${preconditions.pizzaTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating tax`() {
        client.post()
            .uri("/api/workspaces/995943/general-taxes")
            .sendJson(preconditions.defaultNewTax())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new tax`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes")
            .sendJson(
                """{
                    "title": "new space tax",
                    "description": "new space tax description",
                    "rateInBps": 42
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "new space tax",
                    id: "${JsonValues.ANY_NUMBER}",
                    description: "new space tax description",
                    rateInBps: 42,
                    version: 0
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should create a new tax with minimum data`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes")
            .sendJson(
                """{
                    "title": "new space tax",
                    "rateInBps": 42
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "new space tax",
                    id: "${JsonValues.ANY_NUMBER}",
                    rateInBps: 42,
                    version: 0
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating tax`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes")
            .sendJson(preconditions.defaultNewTax())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes/${preconditions.firstSpaceTax.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update tax of current user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/general-taxes/${preconditions.firstSpaceTax.id}")
            .sendJson(
                """{
                    "title": "updated tax",
                    "rateInBps": 42,
                    "description": "updated description"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    title: "updated tax",
                    id: ${preconditions.firstSpaceTax.id},
                    rateInBps: 42,
                    description: "updated description",
                    version: 1
                }"""
            )
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val planetExpressWorkspace = workspace(owner = fry)
            val pizzaDeliveryWorkspace = workspace(owner = fry)
            val pizzaTax = generalTax(workspace = pizzaDeliveryWorkspace)
            val firstSpaceTax = generalTax(
                workspace = planetExpressWorkspace,
                title = "first space tax",
                rateInBps = 4503
            )
            val secondSpaceTax = generalTax(
                workspace = planetExpressWorkspace,
                title = "second space tax",
                description = "second tax description",
                rateInBps = 3
            )

            fun defaultNewTax(): String = """{
                    "title": "new tax",
                    "description": "new tax description",
                    "rateInBps": 1001
                }"""
        }
    }
}
