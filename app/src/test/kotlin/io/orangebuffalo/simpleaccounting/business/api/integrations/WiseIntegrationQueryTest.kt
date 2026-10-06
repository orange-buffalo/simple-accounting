package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WiseIntegrationQueryTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val leela = platformUser(userName = "Leela")
            val farnsworth = farnsworth()
            val adminWorkspace = workspace(owner = farnsworth)
            val workspace = workspace(owner = fry)
            val shared = workspaceAccessToken(workspace = workspace, validTill = MOCK_TIME.plusSeconds(10000))
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should reject anonymous requests`() {
            client.graphql { integrationStatus() }.fromAnonymous().executeAndVerifyNotAuthorized(path = "workspace")
        }

        @Test
        fun `should reject administrators`() {
            client.graphql { integrationStatus(preconditions.adminWorkspace.id!!) }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(paths = listOf("workspace", "integrations"), locationColumn = 5, locationLine = 3)
        }

        @Test
        fun `should reject shared workspace sessions`() {
            client.graphql { integrationStatus() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(paths = listOf("workspace", "integrations"), locationColumn = 5, locationLine = 3)
        }

        @Test
        fun `should reject another users workspace`() {
            client.graphql { integrationStatus() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "workspace")
        }

        @Test
        fun `should reject integrations for a saved shared workspace`() {
            client.graphqlMutation { saveSharedWorkspace(token = preconditions.shared.token) { id } }
                .from(preconditions.leela)
                .executeAndVerifySuccessResponse("saveSharedWorkspace" to json("""{"id":"${preconditions.workspace.id}"}"""))
            client.graphql { integrationStatus() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "workspace", locationColumn = 5, locationLine = 3)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should report inactive Wise integration`() {
            client.graphql { integrationStatus() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("workspace" to json("""{"integrations":{"wise":{"active":false}}}"""))
        }

        @Test
        fun `should report activation only for the configured workspace`() {
            preconditions {
                aggregateTemplate.insert(WiseIntegrationSettings(preconditions.workspace.id!!, "fry-personal-token",
                    setOf(WiseIntegrationAccount(101, 302, "EUR"))))
            }
            client.graphql { integrationStatus() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("workspace" to json("""{"integrations":{"wise":{"active":true}}}"""))
            val other = preconditions { workspace(owner = preconditions.fry) }
            client.graphql { integrationStatus(other.id!!) }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("workspace" to json("""{"integrations":{"wise":{"active":false}}}"""))
        }
    }

    private fun QueryProjection.integrationStatus(workspaceId: String = preconditions.workspace.id!!) =
        workspace(id = workspaceId) { integrations { wise { active } } }

    private fun json(value: String) = Json.parseToJsonElement(value) as JsonObject
}
