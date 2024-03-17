package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyNotFound
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Taxes API ")
internal class GeneralTaxesApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should allow GET access only for logged in users`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return taxes of a workspace of current user`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            title: "first space tax",
                            id: ${testData.firstSpaceTax.id},
                            version: 0,
                            rateInBps: 4503
                    }"""
                    ),

                    json(
                        """{
                            title: "second space tax",
                            id: ${testData.secondSpaceTax.id},
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
    fun `should return 404 if workspace is not found on GET`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/27347947239/general-taxes")
            .verifyNotFound("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 on GET if workspace belongs to another user`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow GET access for tax only for logged in users`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/expenses/${testData.firstSpaceTax.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return tax by id for current user`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes/${testData.firstSpaceTax.id}")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            title: "first space tax",
                            id: ${testData.firstSpaceTax.id},
                            rateInBps: 4503,
                            version: 0
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when requesting tax by id`(testData: GeneralTaxesApiTestData) {
        client.get()
            .uri("/api/workspaces/5634632/general-taxes/${testData.firstSpaceTax.id}")
            .verifyNotFound("Workspace 5634632 is not found")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 404 if workspace belongs to another user when requesting tax by id`(
        testData: GeneralTaxesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes/${testData.firstSpaceTax.id}")
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if tax belongs to another workspace when requesting tax by id`(
        testData: GeneralTaxesApiTestData
    ) {
        client.get()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes/${testData.pizzaTax.id}")
            .verifyNotFound("Tax ${testData.pizzaTax.id} is not found")
    }

    @Test
    @WithMockFryUser
    fun `should return 404 if workspace is not found when creating tax`(testData: GeneralTaxesApiTestData) {
        client.post()
            .uri("/api/workspaces/995943/general-taxes")
            .sendJson(testData.defaultNewTax())
            .verifyNotFound("Workspace 995943 is not found")
    }

    @Test
    @WithMockFryUser
    fun `should create a new tax`(testData: GeneralTaxesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes")
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
    fun `should create a new tax with minimum data`(testData: GeneralTaxesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes")
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
    fun `should return 404 if workspace belongs to another user when creating tax`(testData: GeneralTaxesApiTestData) {
        client.post()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes")
            .sendJson(testData.defaultNewTax())
            .verifyNotFound("Workspace ${testData.planetExpressWorkspace.id} is not found")
    }

    @Test
    fun `should allow PUT access only for logged in users`(testData: GeneralTaxesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes/${testData.firstSpaceTax.id}")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update tax of current user`(testData: GeneralTaxesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.planetExpressWorkspace.id}/general-taxes/${testData.firstSpaceTax.id}")
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
                            id: ${testData.firstSpaceTax.id},
                            rateInBps: 42,
                            description: "updated description",
                            version: 1
                    }"""
                    )
                )
            }
    }

    class GeneralTaxesApiTestData : TestDataDeprecated {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val planetExpressWorkspace = Prototypes.workspace(owner = fry)
        val pizzaDeliveryWorkspace = Prototypes.workspace(owner = fry)
        val pizzaTax = Prototypes.generalTax(workspace = pizzaDeliveryWorkspace)
        val firstSpaceTax = Prototypes.generalTax(
            workspace = planetExpressWorkspace,
            title = "first space tax",
            rateInBps = 4503
        )
        val secondSpaceTax = Prototypes.generalTax(
            workspace = planetExpressWorkspace,
            title = "second space tax",
            description = "second tax description",
            rateInBps = 3
        )

        override fun generateData() = listOf(
            farnsworth, fry, planetExpressWorkspace,
            firstSpaceTax, secondSpaceTax,
            pizzaDeliveryWorkspace, pizzaTax
        )

        fun defaultNewTax(): String = """{
                    "title": "new tax",
                    "description": "new tax description",
                    "rateInBps": 1001
                }"""
    }
}
