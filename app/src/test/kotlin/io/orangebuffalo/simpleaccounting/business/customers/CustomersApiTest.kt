package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@DisplayName("Customers API")
internal class CustomersApiTest(
    @Autowired private val client: WebTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return customers of a workspace of current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers")
            .verifyOkAndJsonBodyEqualTo {
                put("pageNumber", 1)
                put("pageSize", 10)
                put("totalElements", 2)
                putJsonArray("data") {
                    addJsonObject {
                        put("name", "second space customer")
                        put("id", preconditions.secondSpaceCustomer.id)
                        put("version", 0)
                    }
                    addJsonObject {
                        put("name", "first space customer")
                        put("id", preconditions.firstSpaceCustomer.id)
                        put("version", 0)
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
            .uri("/api/workspaces/27347947239/customers")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for customer only for logged in users`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/expenses/${preconditions.firstSpaceCustomer.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return customer by id for current user`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers/${preconditions.firstSpaceCustomer.id}")
            .verifyOkAndJsonBody(
                """{
                    name: "first space customer",
                    id: ${preconditions.firstSpaceCustomer.id},
                    version: 0
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting customer by id`() {
        client.get()
            .uri("/api/workspaces/5634632/customers/${preconditions.firstSpaceCustomer.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting customer by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers/${preconditions.firstSpaceCustomer.id}")
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if customer belongs to another workspace when requesting customer by id`() {
        client.get()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers/${preconditions.pizzaCustomer.id}")
            .verifyNotFound("Customer ${preconditions.pizzaCustomer.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating customer`() {
        client.post()
            .uri("/api/workspaces/995943/customers")
            .sendJson(preconditions.defaultNewCustomer())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new customer`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers")
            .sendJson(
                """{
                    "name": "new space customer"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    name: "new space customer",
                    id: "${JsonValues.ANY_NUMBER}",
                    version: 0
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when creating customer`() {
        client.post()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers")
            .sendJson(preconditions.defaultNewCustomer())
            .verifyNotFound("Workspace ${preconditions.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers/${preconditions.firstSpaceCustomer.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update customer of current user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.planetExpressWorkspace.id}/customers/${preconditions.firstSpaceCustomer.id}")
            .sendJson(
                """{
                    "name": "updated customer"
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    name: "updated customer",
                    id: ${preconditions.firstSpaceCustomer.id},
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
}
