package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
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
class VerifyWiseIntegrationTokenMutationTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
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
        fun `should reject saved shared workspace access without contacting Wise or persisting`() {
            client.graphqlMutation { saveSharedWorkspace(token = preconditions.shared.token) { id } }
                .from(preconditions.leela)
                .executeAndVerifySuccessResponse("saveSharedWorkspace" to buildJsonObject {
                    put("id", preconditions.workspace.id)
                })
            client.graphqlMutation { verifyToken() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "verifyWiseIntegrationToken")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject anonymous requests`() {
            client.graphqlMutation { verifyToken() }.fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "verifyWiseIntegrationToken")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject administrators`() {
            client.graphqlMutation { verifyToken() }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = "verifyWiseIntegrationToken")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject shared workspace sessions`() {
            client.graphqlMutation { verifyToken() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "verifyWiseIntegrationToken")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject another users workspace`() {
            client.graphqlMutation { verifyToken() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "verifyWiseIntegrationToken")
            WiseApiMocks.shouldHaveNoRequests()
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should reject blank tokens without contacting Wise`() {
            for (token in listOf("", " ")) {
                client.graphqlMutation { verifyToken(token) }.from(preconditions.fry)
                    .executeAndVerifyValidationError(
                        violationPath = "token", error = "MustNotBeBlank", message = "must not be blank",
                        path = "verifyWiseIntegrationToken",
                    )
            }
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should verify all profiles and both balance types without saving the token`() {
            WiseApiMocks.setupAccounts()
            client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("verifyWiseIntegrationToken" to (Json.parseToJsonElement("""{
                  "accounts":[
                    {"profileId":"101","profileName":"Philip J. Fry","accountId":"301","currency":"USD","name":null,"type":"STANDARD"},
                    {"profileId":"101","profileName":"Philip J. Fry","accountId":"302","currency":"EUR","name":"Slurm fund","type":"SAVINGS"},
                    {"profileId":"202","profileName":"Planet Express","accountId":"401","currency":"GBP","name":null,"type":"STANDARD"}
                  ]
                }""") as JsonObject))
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should map rejected tokens and provider failures`() {
            for (status in listOf(401, 403, 429, 503)) {
                WiseApiMocks.rejectToken(status)
                val expected = if (status == 401 || status == 403) "INVALID_TOKEN" else "UNAVAILABLE"
                client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = expected, path = "verifyWiseIntegrationToken")
            }
            WiseApiMocks.setupAccounts()
            WiseApiMocks.failBalances()
            client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "UNAVAILABLE", path = "verifyWiseIntegrationToken")
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }
    }

    private fun MutationProjection.verifyToken(token: String = WiseApiMocks.TOKEN) =
        verifyWiseIntegrationToken(workspaceId = preconditions.workspace.id!!, token = token) {
            accounts { profileId; profileName; accountId; currency; name; type }
        }
}
