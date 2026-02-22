package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WorkspacesQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry, name = "Planet Express").also {
                val delivery = category(name = "Delivery", workspace = it)
                val maintenance = category(name = "Robot maintenance", workspace = it)
                expense(title = "Slurm supplies", workspace = it, category = delivery)
                expense(title = "Robot oil", workspace = it, category = maintenance)
                expense(title = "Spaceship parts", workspace = it, category = null)
            }
            val zoidberg = zoidberg().withWorkspace()
            val workspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Test
    fun `should return error when accessed anonymously`() {
        client.graphql {
            workspaces {
                name
            }
        }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.QUERY.Workspaces,
            )
    }

    @Test
    fun `should prohibit access with workspace token`() {
        client.graphql {
            workspaces {
                name
            }
        }
            .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.QUERY.Workspaces,
            )
    }

    @Test
    fun `should return only current user workspaces`() {
        client.graphql {
            workspaces {
                name
            }
        }
            .from(preconditions.fry)
            .executeAndVerifyResponse(
                "workspaces" to buildJsonArray {
                    add(buildJsonObject {
                        put("name", "Planet Express")
                    })
                }
            )
    }

    @Test
    fun `should return workspaces with categories and expenses with categories`() {
        client.graphql {
            workspaces {
                name
                categories { name }
                expenses {
                    title
                    category { name }
                }
            }
        }
            .from(preconditions.fry)
            .executeAndVerifyResponse(
                "workspaces" to buildJsonArray {
                    add(buildJsonObject {
                        put("name", "Planet Express")
                        putJsonArray("categories") {
                            add(buildJsonObject { put("name", "Delivery") })
                            add(buildJsonObject { put("name", "Robot maintenance") })
                        }
                        putJsonArray("expenses") {
                            add(buildJsonObject {
                                put("title", "Slurm supplies")
                                put("category", buildJsonObject { put("name", "Delivery") })
                            })
                            add(buildJsonObject {
                                put("title", "Robot oil")
                                put("category", buildJsonObject { put("name", "Robot maintenance") })
                            })
                            add(buildJsonObject {
                                put("title", "Spaceship parts")
                                put("category", null as String?)
                            })
                        }
                    })
                }
            )
    }
}
