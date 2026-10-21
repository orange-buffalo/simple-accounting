package io.orangebuffalo.simpleaccounting.business.api.integrations

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.infra.graphql.client.types.WiseAccountInput
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners

@DisplayName("Save Wise integration settings mutation")
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
class SaveWiseIntegrationSettingsMutationTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
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
                    put(key = "id", value = preconditions.workspace.id)
                })
            client.graphqlMutation { setup() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "saveWiseIntegrationSettings")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject anonymous requests without contacting Wise`() {
            client.graphqlMutation { setup() }.fromAnonymous()
                .executeAndVerifyNotAuthorized(path = "saveWiseIntegrationSettings")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject administrators`() {
            client.graphqlMutation { setup() }.from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = "saveWiseIntegrationSettings")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject shared workspace sessions`() {
            client.graphqlMutation { setup() }.usingSharedWorkspaceToken(preconditions.shared.token)
                .executeAndVerifyNotAuthorized(path = "saveWiseIntegrationSettings")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject access to another users workspace without contacting Wise or persisting`() {
            client.graphqlMutation { setup() }.from(preconditions.leela)
                .executeAndVerifyEntityNotFoundError(path = "saveWiseIntegrationSettings")
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should persist selected account tuples that differ only by currency`() {
            WiseApiMocks.setupAccountsWithSharedIds()
            client.graphqlMutation {
                setup(listOf(
                    WiseAccountInput(profileId = "101", accountId = "301", currency = "USD"),
                    WiseAccountInput(profileId = "101", accountId = "301", currency = "EUR"),
                ))
            }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("saveWiseIntegrationSettings" to setupResult(true))
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(WiseIntegrationSettings(
                workspaceId = preconditions.workspace.id!!, token = WiseApiMocks.TOKEN,
                fetchTransactionsPeriodically = true, authenticationRequired = false, lastSuccessfulFetchTime = null,
                accounts = setOf(
                    WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD"),
                    WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "EUR"),
                ),
            ))
        }

        @Test
        fun `should create integration with automatic transaction fetching disabled`() {
            WiseApiMocks.setupAccounts()
            client.graphqlMutation { setup(fetchTransactionsPeriodically = false) }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("saveWiseIntegrationSettings" to setupResult(true))
            val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            saved.fetchTransactionsPeriodically.shouldBe(false)
            saved.token.shouldBe(WiseApiMocks.TOKEN)
            saved.accounts.shouldBe(setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR"), WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP")))
        }

        @Test
        fun `should update fetching preferences and resume suspended integrations without losing checkpoints`() {
            val original = preconditions {
                wiseIntegrationSettings(workspace = preconditions.workspace, token = WiseApiMocks.TOKEN,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR", lastSuccessfulFetchTime = MOCK_TIME)),
                    authenticationRequired = true, lastSuccessfulFetchTime = MOCK_TIME)
            }
            WiseApiMocks.setupAccounts()
            client.graphqlMutation { setup(token = null, fetchTransactionsPeriodically = false) }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("saveWiseIntegrationSettings" to setupResult(true))
            val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            saved.id.shouldBe(original.id)
            saved.token.shouldBe(WiseApiMocks.TOKEN)
            saved.fetchTransactionsPeriodically.shouldBe(false)
            saved.authenticationRequired.shouldBe(false)
            saved.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
            saved.accounts.single { it.currency == "EUR" }.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
            saved.accounts.single { it.currency == "GBP" }.lastSuccessfulFetchTime.shouldBe(null)
        }

        @Test
        fun `should reject blank tokens without contacting Wise`() {
            for (token in listOf("", " ")) {
                client.graphqlMutation { setup(token = token) }
                    .from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_TOKEN", path = "saveWiseIntegrationSettings")
            }
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should reject empty account selections without contacting Wise`() {
            client.graphqlMutation { setup(emptyList()) }.from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "accounts", error = "SizeConstraintViolated",
                    message = "size must be between 1 and 2147483647", path = "saveWiseIntegrationSettings",
                    params = mapOf("min" to "1", "max" to "2147483647"),
                )
            WiseApiMocks.shouldHaveNoRequests()
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }

        @Test
        fun `should update accounts with the saved credential`() {
            val persisted = preconditions {
                wiseIntegrationSettings(
                    workspace = preconditions.workspace,
                    token = WiseApiMocks.TOKEN,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR")),
                    authenticationRequired = true,
                    fetchTransactionsPeriodically = false,
                )
            }
            WiseApiMocks.setupAccounts()
            val selection = listOf(WiseAccountInput(profileId = "101", accountId = "301", currency = "USD"))
            client.graphqlMutation { setup(selected = selection, token = null) }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("saveWiseIntegrationSettings" to setupResult(true))
            val updated = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            updated.id.shouldBe(persisted.id)
            updated.token.shouldBe(WiseApiMocks.TOKEN)
            updated.accounts.shouldBe(setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")))
            updated.fetchTransactionsPeriodically.shouldBe(true)
            updated.authenticationRequired.shouldBe(false)
        }

        @Test
        fun `should replace the saved credential`() {
            val persisted = preconditions {
                wiseIntegrationSettings(
                    workspace = preconditions.workspace,
                    token = WiseApiMocks.TOKEN,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")),
                )
            }
            val replacementToken = "bender-replacement-token"
            WiseApiMocks.setupAccounts(token = replacementToken)
            client.graphqlMutation { setup(token = replacementToken) }.from(preconditions.fry)
                .executeAndVerifySuccessResponse("saveWiseIntegrationSettings" to setupResult(true))
            val replaced = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            replaced.id.shouldBe(persisted.id)
            replaced.token.shouldBe(replacementToken)
            replaced.accounts.shouldBe(setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR"), WiseIntegrationAccount(profileId = 202, accountId = 401, currency = "GBP")))
        }

        @Test
        fun `should reject removed accounts without changing saved settings`() {
            val original = preconditions {
                wiseIntegrationSettings(
                    workspace = preconditions.workspace,
                    token = WiseApiMocks.TOKEN,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR")),
                )
            }
            WiseApiMocks.setupAccounts()
            WiseApiMocks.removeEuroBalance()
            client.graphqlMutation { setup(token = null) }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_ACCOUNTS", path = "saveWiseIntegrationSettings")
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBe(original)
        }

        @Test
        fun `should reject invalid replacement tokens without changing saved settings`() {
            val original = preconditions {
                wiseIntegrationSettings(
                    workspace = preconditions.workspace,
                    token = WiseApiMocks.TOKEN,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR")),
                    authenticationRequired = true,
                    fetchTransactionsPeriodically = false,
                    lastSuccessfulFetchTime = MOCK_TIME,
                )
            }
            WiseApiMocks.rejectToken()
            client.graphqlMutation { setup(token = "bender-revoked-token") }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_TOKEN", path = "saveWiseIntegrationSettings")
            val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            saved.id.shouldBe(original.id)
            saved.token.shouldBe(original.token)
            saved.accounts.shouldBe(original.accounts)
            saved.authenticationRequired.shouldBe(true)
            saved.fetchTransactionsPeriodically.shouldBe(false)
            saved.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
        }

        @Test
        fun `should reject forged profile account currency and duplicate selections without changing state`() {
            WiseApiMocks.setupAccounts()
            listOf(
                listOf(WiseAccountInput(profileId = "202", accountId = "302", currency = "EUR")),
                listOf(WiseAccountInput(profileId = "101", accountId = "999", currency = "USD")),
                listOf(WiseAccountInput(profileId = "101", accountId = "303", currency = "CAD")),
                listOf(WiseAccountInput(profileId = "101", accountId = "302", currency = "GBP")),
                listOf(
                    WiseAccountInput(profileId = "101", accountId = "302", currency = "EUR"),
                    WiseAccountInput(profileId = "101", accountId = "302", currency = "EUR"),
                ),
            ).forEach { selection ->
                client.graphqlMutation { setup(selection) }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = "INVALID_ACCOUNTS", path = "saveWiseIntegrationSettings")
                aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
            }
        }

        @Test
        fun `should not persist when listing profiles fails`() {
            for (status in listOf(401, 403, 429, 503)) {
                WiseApiMocks.rejectToken(status)
                val expected = if (status == 401 || status == 403) "INVALID_TOKEN" else "UNAVAILABLE"
                client.graphqlMutation { setup() }.from(preconditions.fry)
                    .executeAndVerifyBusinessErrorCode(errorCode = expected, path = "saveWiseIntegrationSettings")
                aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
            }
        }

        @Test
        fun `should not persist when listing one profiles balances fails`() {
            WiseApiMocks.setupAccounts()
            WiseApiMocks.failBalances()
            client.graphqlMutation { setup() }.from(preconditions.fry)
                .executeAndVerifyBusinessErrorCode(errorCode = "UNAVAILABLE", path = "saveWiseIntegrationSettings")
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        }
    }

    private fun MutationProjection.setup(
        selected: List<WiseAccountInput> = listOf(
            WiseAccountInput(profileId = "101", accountId = "302", currency = "EUR"),
            WiseAccountInput(profileId = "202", accountId = "401", currency = "GBP"),
        ),
        token: String? = WiseApiMocks.TOKEN,
        fetchTransactionsPeriodically: Boolean? = null,
    ) = saveWiseIntegrationSettings(accounts = selected, token = token, workspaceId = preconditions.workspace.id!!,
        fetchTransactionsPeriodically = fetchTransactionsPeriodically) { success }

    private fun setupResult(success: Boolean) = buildJsonObject {
        put(key = "success", value = success)
    }

}
