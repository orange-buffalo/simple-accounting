package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.infra.graphql.client.types.WiseAccountInput
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
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

@DisplayName("Wise integration API")
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
class WiseIntegrationGqlApiTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
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
        fun `should reject anonymous requests for all operations without contacting Wise`() {
            client.graphqlMutation { verifyToken() }.fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "verifyWiseIntegrationToken")
            client.graphqlMutation { setup() }.fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "setupWiseIntegration")
            client.graphql { integrationStatus() }.fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "integrations")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject administrators for all operations`() {
            client.graphqlMutation { verifyToken() }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = "verifyWiseIntegrationToken")
            client.graphqlMutation { setup() }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = "setupWiseIntegration")
            client.graphql { integrationStatus() }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = "integrations")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject shared workspace sessions for all operations`() {
            client.graphqlMutation { verifyToken() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "verifyWiseIntegrationToken")
            client.graphqlMutation { setup() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "setupWiseIntegration")
            client.graphql { integrationStatus() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "integrations")
            WiseApiMocks.shouldHaveNoRequests()
        }

        @Test
        fun `should reject access to another users workspace without contacting Wise or persisting`() {
            client.graphqlMutation { verifyToken() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "verifyWiseIntegrationToken")
            client.graphqlMutation { setup() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "setupWiseIntegration")
            client.graphql { integrationStatus() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "integrations")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should reject blank tokens and empty account selections without contacting Wise`() {
            for (token in listOf("", " ")) {
                client.graphqlMutation { verifyToken(token) }.from(preconditions.fry)
                    .executeAndVerifyValidationError(
                        violationPath = "token", error = "MustNotBeBlank", message = "must not be blank",
                        path = "verifyWiseIntegrationToken",
                    )
                client.graphqlMutation { setup(token = token) }
                    .from(preconditions.fry).executeAndVerifyValidationError(
                        violationPath = "token", error = "MustNotBeBlank", message = "must not be blank",
                        path = "setupWiseIntegration",
                    )
            }
            client.graphqlMutation { setup(emptyList()) }.from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "accounts", error = "SizeConstraintViolated",
                    message = "size must be between 1 and 2147483647", path = "setupWiseIntegration",
                    params = mapOf("min" to "1", "max" to "2147483647"),
                )
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should verify every profile and both balance types without saving the token`() {
            WiseApiMocks.accounts()
            client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("verifyWiseIntegrationToken" to json("""{
                  "accounts":[
                    {"profileId":"101","profileName":"Philip J. Fry","accountId":"301","currency":"USD","name":null,"type":"STANDARD"},
                    {"profileId":"101","profileName":"Philip J. Fry","accountId":"302","currency":"EUR","name":"Slurm fund","type":"SAVINGS"},
                    {"profileId":"202","profileName":"Planet Express","accountId":"401","currency":"GBP","name":null,"type":"STANDARD"}
                  ]
                }"""))
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should save only selected accounts and report activation for this workspace`() {
            WiseApiMocks.accounts()
            client.graphql { integrationStatus() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("integrations" to json("""{"wise":{"active":false}}"""))
            client.graphqlMutation { setup() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("setupWiseIntegration" to setupResult(true))
            val persisted = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            persisted.workspaceId.shouldBe(preconditions.workspace.id)
            persisted.token.shouldBe(WiseApiMocks.TOKEN)
            persisted.accounts.shouldBe(setOf(WiseIntegrationAccount(101, 302, "EUR"), WiseIntegrationAccount(202, 401, "GBP")))
            client.graphql { integrationStatus() }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("integrations" to json("""{"wise":{"active":true}}"""))
            val other = preconditions { workspace(owner = preconditions.fry) }
            client.graphql { integrationStatus(other.id!!) }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("integrations" to json("""{"wise":{"active":false}}"""))
            client.graphqlMutation { setup(token = "bender-replacement-token") }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "ALREADY_ACTIVE", path = "setupWiseIntegration")
            val unchanged = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            unchanged.id.shouldBe(persisted.id)
            unchanged.token.shouldBe(WiseApiMocks.TOKEN)
            unchanged.accounts.shouldBe(setOf(WiseIntegrationAccount(101, 302, "EUR"), WiseIntegrationAccount(202, 401, "GBP")))
        }

        @Test
        fun `should reject forged profile account currency and duplicate selections without changing state`() {
            WiseApiMocks.accounts()
            listOf(
                listOf(WiseAccountInput(profileId = "202", accountId = "302", currency = "EUR")),
                listOf(WiseAccountInput(profileId = "101", accountId = "999", currency = "USD")),
                listOf(WiseAccountInput(profileId = "101", accountId = "302", currency = "GBP")),
                listOf(
                    WiseAccountInput(profileId = "101", accountId = "302", currency = "EUR"),
                    WiseAccountInput(profileId = "101", accountId = "302", currency = "EUR"),
                ),
            ).forEach { selection ->
                client.graphqlMutation { setup(selection) }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_ACCOUNTS", path = "setupWiseIntegration")
                aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
            }
        }

        @Test
        fun `should not persist when token verification or one profile fails`() {
            for (status in listOf(401, 403, 429, 503)) {
                WiseApiMocks.rejectToken(status)
                val expected = if (status == 401 || status == 403) "INVALID_TOKEN" else "UNAVAILABLE"
                client.graphqlMutation { verifyToken() }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = expected, path = "verifyWiseIntegrationToken")
                client.graphqlMutation { setup() }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = expected, path = "setupWiseIntegration")
                aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
            }
            WiseApiMocks.accounts()
            WiseApiMocks.failBalances()
            client.graphqlMutation { setup() }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "UNAVAILABLE", path = "setupWiseIntegration")
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }
    }

    private fun MutationProjection.verifyToken(token: String = WiseApiMocks.TOKEN) = verifyWiseIntegrationToken(workspaceId = preconditions.workspace.id!!, token = token) {
        accounts { profileId; profileName; accountId; currency; name; type }
    }

    private fun MutationProjection.setup(
        selected: List<WiseAccountInput> = listOf(
            WiseAccountInput(profileId = "101", accountId = "302", currency = "EUR"),
            WiseAccountInput(profileId = "202", accountId = "401", currency = "GBP"),
        ),
        token: String = WiseApiMocks.TOKEN,
    ) = setupWiseIntegration(selected, token, preconditions.workspace.id!!) { success }

    private fun setupResult(success: Boolean) = buildJsonObject {
        put("success", success)
    }

    private fun QueryProjection.integrationStatus(workspaceId: String = preconditions.workspace.id!!) =
        integrations(workspaceId) { wise { active } }

    private fun json(value: String) = Json.parseToJsonElement(value) as JsonObject
}
