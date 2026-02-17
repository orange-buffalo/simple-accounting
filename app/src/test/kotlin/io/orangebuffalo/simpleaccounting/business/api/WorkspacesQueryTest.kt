package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WorkspacesQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should return workspaces with categories and expenses with categories`() {
        val preconditions = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry, name = "Planet Express").also {
                    val delivery = category(name = "Delivery", workspace = it)
                    val maintenance = category(name = "Robot maintenance", workspace = it)
                    expense(title = "Slurm supplies", workspace = it, category = delivery)
                    expense(title = "Robot oil", workspace = it, category = maintenance)
                    expense(title = "Spaceship parts", workspace = it, category = null)
                }
            }
        }

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
