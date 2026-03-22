package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WorkspaceQueryTest(
    @Autowired private val client: ApiTestClient,
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
            val zoidberg = zoidberg()
            val zoidbergWorkspace = workspace(owner = zoidberg, name = "Zoidberg Clinic")
            val fryWorkspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidbergWorkspaceToken = workspaceAccessToken(
                workspace = zoidbergWorkspace,
                token = "zoidbergToken",
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return error when accessed anonymously`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    name
                }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.Workspace,
                )
        }

        @Test
        fun `should allow access with workspace token for the token workspace`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    name
                }
            }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("name", "Planet Express")
                    }
                )
        }

        @Test
        fun `should return error when workspace token is for a different workspace`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    name
                }
            }
                .usingSharedWorkspaceToken(preconditions.zoidbergWorkspaceToken.token)
                .executeAndVerifyEntityNotFoundError(
                    path = DgsConstants.QUERY.Workspace,
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return workspace by id for the owner`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    name
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    "workspace" to buildJsonObject {
                        put("name", "Planet Express")
                    }
                )
        }

        @Test
        fun `should return workspace with categories and expenses`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
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
                    "workspace" to buildJsonObject {
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
                    }
                )
        }

        @Test
        fun `should return error when workspace belongs to another user`() {
            client.graphql {
                workspace(id = preconditions.fryWorkspace.id!!.toInt()) {
                    name
                }
            }
                .from(preconditions.zoidberg)
                .executeAndVerifyEntityNotFoundError(
                    path = DgsConstants.QUERY.Workspace,
                )
        }

        @Test
        fun `should return error when workspace does not exist`() {
            client.graphql {
                workspace(id = -1) {
                    name
                }
            }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(
                    path = DgsConstants.QUERY.Workspace,
                )
        }
    }
}
