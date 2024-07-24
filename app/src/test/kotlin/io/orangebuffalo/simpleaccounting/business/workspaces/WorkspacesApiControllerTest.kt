package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockZoidbergUser
import io.orangebuffalo.simpleaccounting.infra.security.WithSaMockUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.infra.TimeService
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
internal class WorkspacesApiControllerTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val timeService: TimeService,
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/workspaces")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return workspaces of current user`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        client.get()
            .uri("/api/workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                            name: "Property of Philip J. Fry",
                            id: ${preconditions.fryWorkspace.id},
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
    fun `should return empty list if no workspace exists for user`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        client.get()
            .uri("/api/workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    @WithSaMockUser(transient = true, workspaceAccessToken = "validFryWorkspaceToken")
    fun `should return shared workspace for transient user on GET workspaces`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        mockCurrentTime(timeService)

        client.get()
            .uri("/api/workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                            name: "Property of Philip J. Fry",
                            id: ${preconditions.fryWorkspace.id},
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
    fun `should create a new workspace`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

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
    fun `should update a workspace`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}")
            .sendJson(
                """{
                    "id": ${preconditions.farnsworthWorkspace.id},
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
                        id: ${preconditions.fryWorkspace.id},
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
    fun `should return 404 on PUT if workspace belongs to another user`() {
        client.put()
            .uri("/api/workspaces/${preconditions.fryWorkspace.id}")
            .sendJson(
                """{
                    "id": ${preconditions.fryWorkspace.id},
                    "name": "wp",
                    "taxEnabled": true,
                    "multiCurrencyEnabled": true,
                    "defaultCurrency": "AUD"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().consumeWith {
                assertThat(it.responseBody).contains("Workspace ${preconditions.fryWorkspace.id} is not found")
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
    fun `should return empty list if no shared workspace exists for user`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        mockCurrentTime(timeService)

        client.get()
            .uri("/api/shared-workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.isEmpty()
            }
    }

    @Test
    @WithMockFryUser
    fun `should return shared workspaces of current user`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        mockCurrentTime(timeService)

        client.get()
            .uri("/api/shared-workspaces")
            .verifyOkAndJsonBody {
                inPath("$").isArray.containsExactly(
                    json(
                        """{
                            name: "Laboratory",
                            id: ${preconditions.farnsworthWorkspace.id},
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
    fun `should save shared workspace by valid access token`() {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/shared-workspaces")
            .sendJson(
                """{
                    "token": "${preconditions.fryWorkspaceAccessToken.token}"
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    json(
                        """{
                            name: "Property of Philip J. Fry",
                            id: ${preconditions.fryWorkspace.id},
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
    fun `should fail on attempt to save shared workspace by expired token`() {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/shared-workspaces")
            .sendJson(
                """{
                    "token": "${preconditions.fryWorkspaceAccessTokenExpired.token}"
                }"""
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<String>().isEqualTo("Token ${preconditions.fryWorkspaceAccessTokenExpired.token} is not valid")
    }

    private val preconditions by preconditionsFactory {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val zoidberg = zoidberg()

            val fryWorkspace = workspace(
                name = "Property of Philip J. Fry",
                owner = fry,
                taxEnabled = false,
                multiCurrencyEnabled = false,
                defaultCurrency = "USD"
            )

            val farnsworthWorkspace = workspace(
                name = "Laboratory",
                owner = farnsworth,
                taxEnabled = false,
                multiCurrencyEnabled = false,
                defaultCurrency = "USD"
            )

            val fryWorkspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plus(Duration.ofDays(1000)),
                token = "validFryWorkspaceToken"
            )

            val fryWorkspaceAccessTokenExpired = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.minusMillis(1),
                token = "expiredFryWorkspaceToken"
            )

            val farnsworthWorkspaceAccessToken = workspaceAccessToken(
                workspace = farnsworthWorkspace,
                validTill = MOCK_TIME.plus(Duration.ofDays(1000)),
                token = "validFarnsworthWorkspaceToken"
            )

            val farnsworthWorkspaceAccessTokenExpired = workspaceAccessToken(
                workspace = farnsworthWorkspace,
                validTill = MOCK_TIME.minusMillis(1),
                token = "expiredFarnsworthWorkspaceToken"
            )

            init {
                save(
                    SavedWorkspaceAccessToken(
                        ownerId = fry.id!!,
                        workspaceAccessTokenId = farnsworthWorkspaceAccessTokenExpired.id!!
                    ),
                    SavedWorkspaceAccessToken(
                        ownerId = fry.id!!,
                        workspaceAccessTokenId = farnsworthWorkspaceAccessToken.id!!
                    )
                )
            }
        }
    }
}
