package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
class ListWiseAccountsMutationTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val leela = platformUser(userName = "Leela")
            val farnsworth = farnsworth()
            val workspace = workspace(owner = fry)
            val shared = workspaceAccessToken(workspace = workspace, validTill = MOCK_TIME.plusSeconds(10000))
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should reject access to the saved credential of another workspace`() {
            preconditions { wiseIntegrationSettings(workspace = preconditions.workspace, token = WiseApiMocks.TOKEN) }
            client.graphqlMutation { verifyToken(token = null) }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject saved shared workspace access without contacting Wise or persisting`() {
            client.graphqlMutation { saveSharedWorkspace(token = preconditions.shared.token) { id } }
                .from(preconditions.leela)
                .executeAndVerifySuccessResponse("saveSharedWorkspace" to buildJsonObject {
                    put(key = "id", value = preconditions.workspace.id)
                })
            client.graphqlMutation { verifyToken() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject anonymous requests`() {
            client.graphqlMutation { verifyToken() }.fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject administrators`() {
            client.graphqlMutation { verifyToken() }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject shared workspace sessions`() {
            client.graphqlMutation { verifyToken() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject another users workspace`() {
            client.graphqlMutation { verifyToken() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should require a token when no credential has been saved`() {
            client.graphqlMutation { verifyToken(token = null) }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_TOKEN", path = "listWiseAccounts")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should load saved selections and deleted accounts without exposing or updating the credential`() {
            preconditions {
                wiseIntegrationSettings(
                    workspace = preconditions.workspace,
                    token = WiseApiMocks.TOKEN,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR"), WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP")),
                )
            }
            val original = aggregateTemplate.findAll<WiseIntegrationSettings>()
            WiseApiMocks.setupAccounts()
            WiseApiMocks.removeJar()
            client.graphqlMutation {
                listWiseAccounts(workspaceId = preconditions.workspace.id!!, token = null) {
                    accounts { profileId; accountId; currency; usedByIntegration; deleted }
                }
            }.from(preconditions.fry).executeAndVerifySuccessResponse("listWiseAccounts" to
                (Json.parseToJsonElement("""{"accounts":[
                    {"profileId":"101","accountId":"301","currency":"USD","usedByIntegration":false,"deleted":false},
                    {"profileId":"202","accountId":"401","currency":"GBP","usedByIntegration":true,"deleted":false},
                    {"profileId":"101","accountId":"302","currency":"EUR","usedByIntegration":true,"deleted":true}
                ]}""") as JsonObject))
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(original)
        }

        @Test
        fun `should reject blank tokens without contacting Wise`() {
            for (token in listOf("", " ")) {
                client.graphqlMutation { verifyToken(token) }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_TOKEN", path = "listWiseAccounts")
            }
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should verify all profiles and both balance types without saving the token`() {
            WiseApiMocks.setupAccounts()
            client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("listWiseAccounts" to (Json.parseToJsonElement("""{
                  "accounts":[
                    {"profileId":"101","profileName":"Philip J. Fry","accountId":"301","currency":"USD","name":null,"type":"STANDARD"},
                    {"profileId":"101","profileName":"Philip J. Fry","accountId":"302","currency":"EUR","name":"Slurm fund","type":"SAVINGS"},
                    {"profileId":"202","profileName":"Planet Express","accountId":"401","currency":"GBP","name":null,"type":"STANDARD"}
                  ]
                }""") as JsonObject))
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should map failures when listing profiles`() {
            for (status in listOf(401, 403, 429, 503)) {
                WiseApiMocks.rejectToken(status)
                val expected = if (status == 401 || status == 403) "INVALID_TOKEN" else "UNAVAILABLE"
                client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = expected, path = "listWiseAccounts")
            }
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should map failure when listing one profiles balances`() {
            WiseApiMocks.setupAccounts()
            WiseApiMocks.failBalances()
            client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "UNAVAILABLE", path = "listWiseAccounts")
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }
    }

    private fun MutationProjection.verifyToken(token: String? = WiseApiMocks.TOKEN) =
        listWiseAccounts(workspaceId = preconditions.workspace.id!!, token = token) {
            accounts { profileId; profileName; accountId; currency; name; type }
        }
}
