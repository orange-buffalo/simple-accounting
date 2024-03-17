package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockZoidbergUser
import io.orangebuffalo.simpleaccounting.infra.security.WithSaMockUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

@SimpleAccountingIntegrationTest
@DisplayName("Workspaces API ")
internal class WorkspacesApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val timeService: TimeService,
) {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return workspaces of current user`(testData: WorkspacesApiTestData) {
        client.get()
            .uri("/api/workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                            name: "Property of Philip J. Fry",
                            id: ${testData.fryWorkspace.id},
                            version: 0,
                            taxEnabled: false,
                            multiCurrencyEnabled: false,
                            defaultCurrency: "USD",
                            editable: true
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockZoidbergUser
    fun `should return empty list if no workspace exists for user`(testData: WorkspacesApiTestData) {
        client.get()
            .uri("/api/workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    @WithSaMockUser(transient = true, workspaceAccessToken = "validFryWorkspaceToken")
    fun `should return shared workspace for transient user on GET workspaces`(testData: WorkspacesApiTestData) {
        mockCurrentTime(timeService)

        client.get()
            .uri("/api/workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                            name: "Property of Philip J. Fry",
                            id: ${testData.fryWorkspace.id},
                            version: 0,
                            taxEnabled: false,
                            multiCurrencyEnabled: false,
                            defaultCurrency: "USD",
                            editable: false
                        }"""
                    )
                )
            }
    }

    @Test
    fun `should allow POST access only for logged in users`() {
        client.post()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should create a new workspace`(testData: WorkspacesApiTestData) {
        client.post()
            .uri("/api/workspaces")
            .sendJson(
                """{
                    "name": "wp",
                    "taxEnabled": false,
                    "multiCurrencyEnabled": true,
                    "defaultCurrency": "GPB"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "wp",
                        id: "#{json-unit.any-number}",
                        version: 0,
                        taxEnabled: false,
                        multiCurrencyEnabled: true,
                        defaultCurrency: "GPB",
                        editable: true
                    }"""
                    )
                )
            }
    }

    @Test
    fun `should allow PUT access only for logged in users`() {
        client.put()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should update a workspace`(testData: WorkspacesApiTestData) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}")
            .sendJson(
                """{
                    "id": ${testData.farnsworthWorkspace.id},
                    "name": "wp",
                    "taxEnabled": true,
                    "multiCurrencyEnabled": true,
                    "defaultCurrency": "AUD"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "wp",
                        id: ${testData.fryWorkspace.id},
                        version: 1,
                        taxEnabled: false,
                        multiCurrencyEnabled: false,
                        defaultCurrency: "USD",
                        editable: true
                    }"""
                    )
                )
            }
    }


    @Test
    @WithMockZoidbergUser
    fun `should return 404 on PUT if workspace belongs to another user`(
        testData: WorkspacesApiTestData
    ) {
        client.put()
            .uri("/api/workspaces/${testData.fryWorkspace.id}")
            .sendJson(
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

    @Test
    fun `should allow POST of shared workspaces only for logged in users`() {
        client.post()
            .uri("/api/shared-workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockZoidbergUser
    fun `should return empty list if no shared workspace exists for user`(testData: WorkspacesApiTestData) {
        mockCurrentTime(timeService)

        client.get()
            .uri("/api/shared-workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    @WithMockFryUser
    fun `should return shared workspaces of current user`(testData: WorkspacesApiTestData) {
        mockCurrentTime(timeService)

        client.get()
            .uri("/api/shared-workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                            name: "Laboratory",
                            id: ${testData.farnsworthWorkspace.id},
                            version: 0,
                            taxEnabled: false,
                            multiCurrencyEnabled: false,
                            defaultCurrency: "USD",
                            editable: false
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should save shared workspace by valid access token`(testData: WorkspacesApiTestData) {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/shared-workspaces")
            .sendJson(
                """{
                    "token": "${testData.fryWorkspaceAccessToken.token}"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            name: "Property of Philip J. Fry",
                            id: ${testData.fryWorkspace.id},
                            version: 0,
                            taxEnabled: false,
                            multiCurrencyEnabled: false,
                            defaultCurrency: "USD",
                            editable: false
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should fail on attempt to save shared workspace by expired token`(testData: WorkspacesApiTestData) {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/shared-workspaces")
            .sendJson(
                """{
                    "token": "${testData.fryWorkspaceAccessTokenExpired.token}"
                }"""
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>().isEqualTo("Token ${testData.fryWorkspaceAccessTokenExpired.token} is not valid")
    }

    class WorkspacesApiTestData : TestDataDeprecated {
        val fry = Prototypes.fry()
        val farnsworth = Prototypes.farnsworth()
        val zoidberg = Prototypes.zoidberg()

        val fryWorkspace = Prototypes.workspace(
            name = "Property of Philip J. Fry",
            owner = fry,
            taxEnabled = false,
            multiCurrencyEnabled = false,
            defaultCurrency = "USD"
        )

        val farnsworthWorkspace = Prototypes.workspace(
            name = "Laboratory",
            owner = farnsworth,
            taxEnabled = false,
            multiCurrencyEnabled = false,
            defaultCurrency = "USD"
        )

        val fryWorkspaceAccessToken = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            validTill = MOCK_TIME.plus(Duration.ofDays(1000)),
            token = "validFryWorkspaceToken"
        )

        val fryWorkspaceAccessTokenExpired = Prototypes.workspaceAccessToken(
            workspace = fryWorkspace,
            validTill = MOCK_TIME.minusMillis(1),
            token = "expiredFryWorkspaceToken"
        )

        val farnsworthWorkspaceAccessToken = Prototypes.workspaceAccessToken(
            workspace = farnsworthWorkspace,
            validTill = MOCK_TIME.plus(Duration.ofDays(1000)),
            token = "validFarnsworthWorkspaceToken"
        )

        val farnsworthWorkspaceSavedForFry = SavedWorkspaceAccessToken(
            ownerId = fry.id!!,
            workspaceAccessTokenId = farnsworthWorkspaceAccessToken.id!!
        )

        val farnsworthWorkspaceAccessTokenExpired = Prototypes.workspaceAccessToken(
            workspace = farnsworthWorkspace,
            validTill = MOCK_TIME.minusMillis(1),
            token = "expiredFarnsworthWorkspaceToken"
        )

        val farnsworthWorkspaceSavedForFryExpired = SavedWorkspaceAccessToken(
            ownerId = fry.id!!,
            workspaceAccessTokenId = farnsworthWorkspaceAccessTokenExpired.id!!
        )

        override fun generateData() = listOf(
            farnsworth, fry, fryWorkspace, farnsworthWorkspace, zoidberg,
            fryWorkspaceAccessToken, fryWorkspaceAccessTokenExpired,
            farnsworthWorkspaceAccessToken, farnsworthWorkspaceSavedForFry,
            farnsworthWorkspaceAccessTokenExpired, farnsworthWorkspaceSavedForFryExpired
        )
    }
}
