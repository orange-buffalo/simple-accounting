package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.tests.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Taxes API")
internal class GeneralTaxesApiTest(
    @Autowired private val client: WebTestClient,
    preconditionsFactory: PreconditionsFactory,
) {

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
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            title: "first space tax",
                            id: ${preconditions.firstSpaceTax.id},
                            version: 0,
                            rateInBps: 4503
                    }"""
                    ),

                    json(
                        """{
                            title: "second space tax",
                            id: ${preconditions.secondSpaceTax.id},
                            rateInBps: 3,
                            description: "second tax description",
                            version: 0
                    }"""
                    )
                )
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
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            title: "first space tax",
                            id: ${preconditions.firstSpaceTax.id},
                            rateInBps: 4503,
                            version: 0
                    }"""
                    )
                )
            }
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
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "new space tax",
                            id: "#{json-unit.any-number}",
                            description: "new space tax description",
                            rateInBps: 42,
                            version: 0
                    }"""
                    )
                )
            }
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
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "new space tax",
                            id: "#{json-unit.any-number}",
                            rateInBps: 42,
                            version: 0
                    }"""
                    )
                )
            }
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
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            title: "updated tax",
                            id: ${preconditions.firstSpaceTax.id},
                            rateInBps: 42,
                            description: "updated description",
                            version: 1
                    }"""
                    )
                )
            }
    }

    private val preconditions by preconditionsFactory {
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
