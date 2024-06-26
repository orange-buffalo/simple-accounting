package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Customers API ")
internal class CustomersApiControllerIT(
    @Autowired private val client: WebTestClient,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    fun `should allow GET access only for logged in users`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return customers of a workspace of current user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            name: "first space customer",
                            id: ${testData.firstSpaceCustomer.id},
                            version: 0
                    }"""
                    ),

                    json(
                        """{
                            name: "second space customer",
                            id: ${testData.secondSpaceCustomer.id},
                            version: 0
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found on GET`() {
        setupPreconditions()
        client.get()
            .uri("/api/workspaces/27347947239/customers")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for customer only for logged in users`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/expenses/${testData.firstSpaceCustomer.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return customer by id for current user`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers/${testData.firstSpaceCustomer.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            name: "first space customer",
                            id: ${testData.firstSpaceCustomer.id},
                            version: 0
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting customer by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/5634632/customers/${testData.firstSpaceCustomer.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting customer by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers/${testData.firstSpaceCustomer.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if customer belongs to another workspace when requesting customer by id`() {
        val testData = setupPreconditions()
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers/${testData.pizzaCustomer.id}")
            .verifyNotFound("Customer ${testData.pizzaCustomer.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating customer`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/995943/customers")
            .sendJson(testData.defaultNewCustomer())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new customer`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers")
            .sendJson(
                """{
                    "name": "new space customer"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            name: "new space customer",
                            id: "#{json-unit.any-number}",
                            version: 0
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating customer`() {
        val testData = setupPreconditions()
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers")
            .sendJson(testData.defaultNewCustomer())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers/${testData.firstSpaceCustomer.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update customer of current user`() {
        val testData = setupPreconditions()
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/customers/${testData.firstSpaceCustomer.id}")
            .sendJson(
                """{
                    "name": "updated customer"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            name: "updated customer",
                            id: ${testData.firstSpaceCustomer.id},
                            version: 1
                    }"""
                    )
                )
            }
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()
        val farnsworth = farnsworth()
        val planetExpressWorkspace = workspace(owner = fry)
        val pizzaDeliveryWorkspace = workspace(owner = fry)
        val pizzaCustomer = customer(workspace = pizzaDeliveryWorkspace)
        val firstSpaceCustomer = customer(
            workspace = planetExpressWorkspace,
            name = "first space customer"
        )
        val secondSpaceCustomer = customer(
            workspace = planetExpressWorkspace,
            name = "second space customer"
        )

        fun defaultNewCustomer(): String = """{
                    "name": "new customer"
                }"""
    }
}
