package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners

@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
class RemoveWiseIntegrationMutationTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val leela = platformUser(userName = "Leela")
            val farnsworth = farnsworth()
            val workspace = workspace(owner = fry)
            val shared = workspaceAccessToken(workspace = workspace, validTill = MOCK_TIME.plusSeconds(10000))
            val settings = wiseIntegrationSettings(workspace = workspace, authenticationRequired = true)
            val statement = accountStatementRecord(settings)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should reject anonymous requests`() {
            mutation().fromAnonymous().executeAndVerifyNotAuthorized(path = "removeWiseIntegration")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject administrators`() {
            mutation().from(preconditions.farnsworth).executeAndVerifyNotAuthorized(path = "removeWiseIntegration")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject shared sessions`() {
            mutation().usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "removeWiseIntegration")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject another workspace owner`() {
            mutation().from(preconditions.leela).executeAndVerifyEntityNotFoundError(path = "removeWiseIntegration")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject saved shared access`() {
            client.graphqlMutation { saveSharedWorkspace(token = preconditions.shared.token) { id } }
                .from(preconditions.leela).executeAndVerifySuccessResponse("saveSharedWorkspace" to buildJsonObject {
                    put(key = "id", value = preconditions.workspace.id)
                })
            mutation().from(preconditions.leela).executeAndVerifyEntityNotFoundError(path = "removeWiseIntegration")
            shouldBeUnchanged()
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should remove only the selected integration and its statements without contacting Wise`() {
            val other = preconditions {
                object {
                    val settings = wiseIntegrationSettings()
                    val statement = accountStatementRecord(settings)
                }
            }
            mutation().from(preconditions.fry).executeAndVerifySuccessResponse("removeWiseIntegration" to success())
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(other.settings)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(other.statement)
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should succeed when integration is already absent`() {
            val data = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphqlMutation { removeWiseIntegration(workspaceId = data.workspace.id!!) { success } }
                .from(data.fry).executeAndVerifySuccessResponse("removeWiseIntegration" to success())
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
            WiseApiMocks.shouldHaveNoRequests()
        }
    }

    private fun mutation() = client.graphqlMutation { removeWiseIntegration(workspaceId = preconditions.workspace.id!!) { success } }

    private fun shouldBeUnchanged() {
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(preconditions.settings)
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(preconditions.statement)
        WiseApiMocks.shouldHaveNoRequests()
    }

    private fun success() = buildJsonObject { put(key = "success", value = true) }
}
