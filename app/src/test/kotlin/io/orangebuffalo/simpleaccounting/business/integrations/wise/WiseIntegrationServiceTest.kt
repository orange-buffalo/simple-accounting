package io.orangebuffalo.simpleaccounting.business.integrations.wise

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorageResetListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldEventually
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class, TestDocumentsStorageResetListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WiseIntegrationServiceTest(
    @Autowired private val service: WiseIntegrationService,
    @Autowired private val transactions: TransactionTemplate,
    @Autowired private val fetcher: WiseStatementFetchService,
    @Autowired private val jdbc: JdbcTemplate,
) : SaIntegrationTestBase() {
    private val token = WiseApiMocks.TOKEN
    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
        }
    }

    private fun verifyToken() = runAs(preconditions.fry.toSecurityPrincipal()) {
        service.verifyToken(workspaceId = preconditions.workspace.id!!, token = token)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should start monthly only fetching after a settings save commits`(existing: Boolean) {
        val data = preconditions { object {
            val fry = platformUser(documentsStorage = TestDocumentsStorage.STORAGE_ID)
            val workspace = workspace(owner = fry).also {
                if (existing) wiseIntegrationSettings(workspace = it, fetchTransactionsPeriodically = false, fetchMonthlyStatements = false)
            }
        } }
        WiseApiMocks.setupAccounts()
        WiseApiMocks.setupStatementPdf()

        runAs(data.fry.toSecurityPrincipal()) {
            transactions.executeWithoutResult {
                service.setup(
                    workspaceId = data.workspace.id!!, token = token,
                    accounts = listOf(WiseAccountSelection(profileId = "101", accountId = "301", currency = "USD")),
                    fetchTransactionsPeriodically = false, fetchMonthlyStatements = true,
                )
                ThirdPartyApisMocks.server.verify(0, getRequestedFor(urlPathEqualTo(WiseApiMocks.STATEMENT_PDF_PATH)))
            }
        }

        shouldEventually {
            aggregateTemplate.findAll<StandaloneDocument>().shouldBeSingle().title
                .shouldBe("Wise Monthly Statement for account USD 301 1999-02-01_1999-02-28")
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(WiseIntegrationSettings(
            workspaceId = data.workspace.id!!, token = token, fetchTransactionsPeriodically = false, fetchMonthlyStatements = true,
            createIncomeFromInvoices = false,
            accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")),
        ))
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        WiseApiMocks.shouldHaveNoTransactionStatementRequests()
        ThirdPartyApisMocks.server.verify(1, getRequestedFor(urlPathEqualTo(WiseApiMocks.STATEMENT_PDF_PATH)))
    }

    @Test
    fun `should not dispatch fetching when both preferences are disabled`() {
        val data = preconditions { object {
            val fry = platformUser(documentsStorage = TestDocumentsStorage.STORAGE_ID)
            val workspace = workspace(owner = fry)
        } }
        WiseApiMocks.setupAccounts()
        WiseApiMocks.setupStatementPdf()

        runAs(data.fry.toSecurityPrincipal()) {
            service.setup(
                workspaceId = data.workspace.id!!, token = token,
                accounts = listOf(WiseAccountSelection(profileId = "101", accountId = "301", currency = "USD")),
                fetchTransactionsPeriodically = false, fetchMonthlyStatements = false,
            )
        }

        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(WiseIntegrationSettings(
            workspaceId = data.workspace.id!!, token = token, fetchTransactionsPeriodically = false, fetchMonthlyStatements = false,
            createIncomeFromInvoices = false,
            accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")),
        ))
        aggregateTemplate.findAll<StandaloneDocument>().shouldBe(emptyList())
        ThirdPartyApisMocks.server.verify(0, getRequestedFor(urlPathEqualTo(WiseApiMocks.STATEMENT_PDF_PATH)))
        WiseApiMocks.shouldHaveNoStatementRequests()
    }

    @Test
    fun `should defer fetching until the enclosing save transaction commits`() {
        WiseApiMocks.setupAccounts()
        WiseApiMocks.setupStatement()
        runAs(preconditions.fry.toSecurityPrincipal()) {
            transactions.executeWithoutResult {
                service.setup(workspaceId = preconditions.workspace.id!!, token = token,
                    accounts = listOf(WiseAccountSelection(profileId = "101", accountId = "301", currency = "USD")))
                WiseApiMocks.shouldHaveNoStatementRequests()
            }
        }
        shouldEventually {
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should not fetch when an enclosing save transaction rolls back`(transactionsEnabled: Boolean) {
        val data = preconditions { object {
            val fry = platformUser(documentsStorage = TestDocumentsStorage.STORAGE_ID)
            val workspace = workspace(owner = fry)
        } }
        WiseApiMocks.setupAccounts()
        WiseApiMocks.setupStatement()
        WiseApiMocks.setupStatementPdf()
        runAs(data.fry.toSecurityPrincipal()) {
            transactions.executeWithoutResult { status ->
                service.setup(workspaceId = data.workspace.id!!, token = token,
                    accounts = listOf(WiseAccountSelection(profileId = "101", accountId = "301", currency = "USD")),
                    fetchTransactionsPeriodically = transactionsEnabled)
                WiseApiMocks.shouldHaveNoStatementRequests()
                status.setRollbackOnly()
            }
        }
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        WiseApiMocks.shouldHaveNoStatementRequests()
        ThirdPartyApisMocks.server.verify(0, getRequestedFor(urlPathEqualTo(WiseApiMocks.STATEMENT_PDF_PATH)))
    }

    @Test
    fun `should serialize settings changes after an in-flight fetch and retain its checkpoint`() {
        val data = preconditions {
            object {
                val fry = fry()
                val integration = wiseIntegrationSettings(
                    workspace = workspace(owner = fry), token = token,
                    accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")),
                )
            }
        }
        WiseApiMocks.setupAccounts()
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val (fetching, saving) = WiseApiMocks.blockStatementResponse().use { blocked ->
                val fetching = executor.submit { fetcher.fetch(data.integration.id!!) }
                blocked.shouldBePending()
                val saving = executor.submit {
                    runAs(data.fry.toSecurityPrincipal()) {
                        service.setup(workspaceId = data.integration.workspaceId, token = null,
                            accounts = listOf(WiseAccountSelection(profileId = "101", accountId = "302", currency = "EUR")),
                            fetchTransactionsPeriodically = false)
                    }
                }
                shouldEventually {
                    jdbc.queryForObject("""
                        select exists (
                            select 1 from pg_stat_activity
                            where datname = current_database()
                              and cardinality(pg_blocking_pids(pid)) > 0
                              and query like '%wise_integration_settings%'
                        )
                    """.trimIndent(), Boolean::class.java).shouldBe(true)
                }
                saving.isDone.shouldBe(false)
                fetching to saving
            }
            fetching.get(10, TimeUnit.SECONDS)
            saving.get(10, TimeUnit.SECONDS)
        }

        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(
            WiseIntegrationSettings(
                workspaceId = data.integration.workspaceId, token = token,
                accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR")),
                fetchTransactionsPeriodically = false, authenticationRequired = false, lastSuccessfulFetchTime = MOCK_TIME,
                createIncomeFromInvoices = false,
            ),
        )
        WiseApiMocks.shouldHaveStatementRequests(count = 1)
        WiseApiMocks.shouldHaveStatementRequests(accountId = 302, count = 0)
    }

    @Test
    fun `should preserve long identifiers and ignore additional provider fields`() {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740993,"fullName":"Bender","type":"PERSONAL"}]""")))
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/9007199254740993/balances?types=STANDARD"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740995,"currency":"USD","type":"STANDARD","amount":{"value":42}}]""")))

        verifyToken().shouldBe(listOf(
            WiseAccount(profileId = 9007199254740993, profileName = "Bender", profileType = "PERSONAL", accountId = 9007199254740995, currency = "USD", name = null, type = "STANDARD", amountInCents = 4200),
        ))
    }

    @Test
    fun `should convert provider balances to currency minor units`() {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles"))
            .willReturn(okJson("""[{"id":101,"fullName":"Philip J. Fry","type":"PERSONAL"}]""")))
        for ((currency, value, expected) in listOf(
            Triple(first = "JPY", second = "1234", third = 1234L),
            Triple(first = "KWD", second = "12.345", third = 12345L),
            Triple(first = "USD", second = "-12.34", third = -1234L),
        )) {
            ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/101/balances?types=STANDARD"))
                .willReturn(okJson("""[{"id":301,"currency":"$currency","type":"STANDARD","amount":{"value":$value}}]""")))
            verifyToken().single().amountInCents.shouldBe(expected)
        }
    }

    @Test
    fun `should reject invalid or unrepresentable balances without retaining provider data`() {
        WiseApiMocks.setupAccounts()
        for ((currency, value) in listOf(
            "USD" to "\"$token\"",
            "USD" to "0.001",
            "USD" to "9223372036854775808",
            "SLURM" to "42",
        )) {
            ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles/101/balances?types=STANDARD"))
                .willReturn(okJson("""[{"id":301,"currency":"$currency","type":"STANDARD","amount":{"value":$value}}]""")))
            val exception = shouldThrow<WiseUnavailableException> { verifyToken() }
            exception.message.shouldBe("Wise accounts could not be loaded")
            exception.cause.shouldBe(null)
        }
    }

    @Test
    fun `should sanitize rejected token and upstream error bodies`() {
        for (status in listOf(401, 403, 429, 503)) {
            ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles")).willReturn(aResponse().withStatus(status).withBody(token)))
            val exception = if (status == 401 || status == 403) {
                shouldThrow<WiseInvalidTokenException> { verifyToken() }
                    .also { it.message.shouldBe("Wise rejected the token") }
            } else {
                shouldThrow<WiseUnavailableException> { verifyToken() }
                    .also { it.message.shouldBe("Wise accounts could not be loaded") }
            }
            exception.cause.shouldBe(null)
        }
    }

    @Test
    fun `should treat malformed responses as unavailable without retaining the payload`() {
        ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles")).willReturn(okJson("""{"unexpected":"$token"}""")))
        val exception = shouldThrow<WiseUnavailableException> { verifyToken() }
        exception.message.shouldBe("Wise accounts could not be loaded")
        exception.cause.shouldBe(null)
    }

    @Test
    fun `should treat empty responses and transport failures as unavailable`() {
        for (response in listOf(aResponse().withStatus(204), aResponse().withFault(Fault.EMPTY_RESPONSE))) {
            ThirdPartyApisMocks.server.stubFor(get(urlEqualTo("/wise/2026Q4/profiles")).willReturn(response))

            val exception = shouldThrow<WiseUnavailableException> { verifyToken() }
            exception.message.shouldBe("Wise accounts could not be loaded")
            exception.cause.shouldBe(null)
        }
    }

}
