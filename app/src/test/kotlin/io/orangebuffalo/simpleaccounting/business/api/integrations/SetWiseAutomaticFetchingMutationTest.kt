package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
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
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldEventually
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
class SetWiseAutomaticFetchingMutationTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val leela = platformUser(userName = "Leela")
            val farnsworth = farnsworth()
            val workspace = workspace(owner = fry)
            val shared = workspaceAccessToken(workspace = workspace, validTill = MOCK_TIME.plusSeconds(10000))
            val settings = wiseIntegrationSettings(
                workspace = workspace, authenticationRequired = true, lastSuccessfulFetchTime = MOCK_TIME,
                accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD", lastSuccessfulFetchTime = MOCK_TIME)),
            )
            val statement = accountStatementRecord(settings)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should reject anonymous requests`() {
            mutation(false).fromAnonymous().executeAndVerifyNotAuthorized(path = "setWiseAutomaticFetching")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject administrators`() {
            mutation(false).from(preconditions.farnsworth).executeAndVerifyNotAuthorized(path = "setWiseAutomaticFetching")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject shared sessions`() {
            mutation(false).usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "setWiseAutomaticFetching")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject another workspace owner`() {
            mutation(false).from(preconditions.leela).executeAndVerifyEntityNotFoundError(path = "setWiseAutomaticFetching")
            shouldBeUnchanged()
        }

        @Test
        fun `should reject saved shared access`() {
            client.graphqlMutation { saveSharedWorkspace(token = preconditions.shared.token) { id } }
                .from(preconditions.leela).executeAndVerifySuccessResponse("saveSharedWorkspace" to buildJsonObject {
                    put(key = "id", value = preconditions.workspace.id)
                })
            mutation(false).from(preconditions.leela).executeAndVerifyEntityNotFoundError(path = "setWiseAutomaticFetching")
            shouldBeUnchanged()
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should reject toggling an integration that is not configured`() {
            val data = preconditions {
                object {
                    val fry = fry()
                    val workspace = workspace(owner = fry)
                }
            }
            client.graphqlMutation { setWiseAutomaticFetching(workspaceId = data.workspace.id!!, enabled = true) { success } }
                .from(data.fry).executeAndVerifyEntityNotFoundError(path = "setWiseAutomaticFetching")
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should disable without changing saved settings or statements or contacting Wise`() {
            mutation(false).from(preconditions.fry).executeAndVerifySuccessResponse("setWiseAutomaticFetching" to success())
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
                .shouldBeEntityWithFields(preconditions.settings.copy(fetchTransactionsPeriodically = false))
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(preconditions.statement)
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should fetch asynchronously after enabling a valid integration`() {
            val data = preconditions {
                object {
                    val fry = fry()
                    val settings = wiseIntegrationSettings(
                        workspace = workspace(owner = fry), token = WiseApiMocks.TOKEN, fetchTransactionsPeriodically = false,
                        accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")),
                    )
                }
            }
            WiseApiMocks.blockStatementResponse().use { blocked ->
                client.graphqlMutation { setWiseAutomaticFetching(workspaceId = data.settings.workspaceId, enabled = true) { success } }
                    .from(data.fry).executeAndVerifySuccessResponse("setWiseAutomaticFetching" to success())
                blocked.shouldBePending()
            }
            shouldEventually {
                aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(
                    data.settings.copy(
                        fetchTransactionsPeriodically = true, lastSuccessfulFetchTime = MOCK_TIME,
                        accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD", lastSuccessfulFetchTime = MOCK_TIME)),
                    ),
                )
            }
        }

        @Test
        fun `should preserve authentication failure when enabling`() {
            val data = preconditions {
                object {
                    val fry = fry()
                    val settings = wiseIntegrationSettings(workspace = workspace(owner = fry), fetchTransactionsPeriodically = false, authenticationRequired = true)
                }
            }
            client.graphqlMutation { setWiseAutomaticFetching(workspaceId = data.settings.workspaceId, enabled = true) { success } }
                .from(data.fry).executeAndVerifySuccessResponse("setWiseAutomaticFetching" to success())
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
                .shouldBeEntityWithFields(data.settings.copy(fetchTransactionsPeriodically = true))
            WiseApiMocks.shouldHaveNoRequests()
        }
    }

    private fun mutation(enabled: Boolean) = client.graphqlMutation {
        setWiseAutomaticFetching(workspaceId = preconditions.workspace.id!!, enabled = enabled) { success }
    }

    private fun shouldBeUnchanged() {
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(preconditions.settings)
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(preconditions.statement)
        WiseApiMocks.shouldHaveNoRequests()
    }

    private fun success() = buildJsonObject { put(key = "success", value = true) }
}
