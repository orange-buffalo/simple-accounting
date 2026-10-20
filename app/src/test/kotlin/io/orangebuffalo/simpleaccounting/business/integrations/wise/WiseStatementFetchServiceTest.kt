package io.orangebuffalo.simpleaccounting.business.integrations.wise

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.StatementDirection
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WiseStatementFetchServiceTest(
    @Autowired private val fetcher: WiseStatementFetchService,
    @Autowired private val job: WiseStatementFetchJob,
    @Autowired private val service: WiseIntegrationService,
    @Autowired private val time: TimeService,
) : SaIntegrationTestBase() {
    private val dollars = WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")
    private val euros = WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR")

    @Test
    fun `should import three calendar months and preserve generic transaction details`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        WiseApiMocks.setupStatement(transactions = """[{
            "type":"DEBIT","date":"1999-03-27T12:00:00Z","referenceNumber":"CARD-249281",
            "amount":{"value":-7.76,"currency":"USD"},"totalFees":{"value":0.04,"currency":"USD"},
            "runningBalance":{"value":16.01,"currency":"USD"},
            "details":{"type":"CARD","description":"Slurm supplies","paymentReference":"Planet Express",
                "merchant":{"name":"Slurm factory","firstLine":"Robot street","city":"New New York","country":"US","category":"Slurm"}},
            "exchangeDetails":{"forAmount":{"value":6.8,"currency":"GBP"},"rate":1.147806},"extra":"ignored"
        }]""")

        fetcher.fetch(integration.id!!)

        assertInterval(start = "1998-12-28T23:01:02.042Z", end = MOCK_TIME.toString())
        val record = aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle()
        record.workspaceId.shouldBe(integration.workspaceId)
        record.provider.shouldBe("WISE")
        record.integrationId.shouldBe(integration.id)
        record.profileId.shouldBe("101")
        record.accountId.shouldBe("301")
        record.accountCurrency.shouldBe("USD")
        record.externalId.shouldBe("CARD-249281")
        record.direction.shouldBe(StatementDirection.DEBIT)
        record.occurredAt.shouldBe(Instant.parse("1999-03-27T12:00:00Z"))
        record.amountInCents.shouldBe(-776)
        record.currency.shouldBe("USD")
        record.feesInCents.shouldBe(4)
        record.feesCurrency.shouldBe("USD")
        record.runningBalanceInCents.shouldBe(1601)
        record.runningBalanceCurrency.shouldBe("USD")
        record.transactionType.shouldBe("CARD")
        record.description.shouldBe("Slurm supplies")
        record.paymentReference.shouldBe("Planet Express")
        record.counterpartyName.shouldBe("Slurm factory")
        record.counterpartyAccount.shouldBe(null)
        record.merchantCategory.shouldBe("Slurm")
        record.merchantAddress.shouldBe("Robot street, New New York, US")
        record.originalAmountInCents.shouldBe(680)
        record.originalCurrency.shouldBe("GBP")
        record.exchangeRate.shouldBe(BigDecimal("1.147806"))
        val updated = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        updated.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
        updated.accounts.single().lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
    }

    @Test
    fun `should overlap seven days and deduplicate repeated transactions while importing late postings and refunds`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        WiseApiMocks.setupStatement(transactions = "[${transaction()}]")
        fetcher.fetch(integration.id!!)
        val originalId = aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().id
        WiseApiMocks.setupStatement(transactions = "[${transaction()},${transaction(reference = "TRANSFER-42")},${transaction(direction = "CREDIT", amount = "1.25")}]")

        fetcher.fetch(integration.id)

        assertInterval(start = "1999-03-21T23:01:02.042Z", end = MOCK_TIME.toString())
        val records = aggregateTemplate.findAll<AccountStatementRecord>()
        records.size.shouldBe(3)
        records.single { it.externalId == "CARD-1" && it.direction == StatementDirection.DEBIT }.id.shouldBe(originalId)
        records.single { it.direction == StatementDirection.CREDIT }.amountInCents.shouldBe(125)
    }

    @Test
    fun `should scope transaction identity to integration and account including account currency`() {
        val integrations = preconditions {
            listOf(
                wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(
                    dollars, dollars.copy(currency = "EUR"), dollars.copy(profileId = 202), dollars.copy(accountId = 303),
                )),
                wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)),
            )
        }
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo(statementPath()))
            .withQueryParam("currency", equalTo("USD"))
            .willReturn(okJson("""{"transactions":[${transaction()}]}""")))
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo(statementPath()))
            .withQueryParam("currency", equalTo("EUR"))
            .willReturn(okJson("""{"transactions":[${transaction().replace(oldValue = "USD", newValue = "EUR")}]}""")))
        WiseApiMocks.setupStatement(profileId = 202, transactions = "[${transaction()}]")
        WiseApiMocks.setupStatement(accountId = 303, transactions = "[${transaction()}]")

        integrations.forEach { fetcher.fetch(it.id!!) }
        integrations.forEach { fetcher.fetch(it.id!!) }

        val records = aggregateTemplate.findAll<AccountStatementRecord>()
        records.map { "${it.integrationId}:${it.profileId}:${it.accountId}:${it.accountCurrency}:${it.externalId}" }.toSet().shouldBe(setOf(
            "${integrations[0].id}:101:301:USD:CARD-1", "${integrations[0].id}:101:301:EUR:CARD-1",
            "${integrations[0].id}:202:301:USD:CARD-1", "${integrations[0].id}:101:303:USD:CARD-1",
            "${integrations[1].id}:101:301:USD:CARD-1",
        ))
        records.size.shouldBe(5)
    }

    @Test
    fun `should map incoming transfers and conversion amounts using each currency precision`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        WiseApiMocks.setupStatement(transactions = """[{
            "type":"CREDIT","date":"1999-03-27T12:00:00Z","referenceNumber":"TRANSFER-42",
            "amount":{"value":42,"currency":"USD"},
            "details":{"type":"CONVERSION","senderName":"Philip J. Fry","senderAccount":"Planet Express account",
                "sourceAmount":{"value":1234,"currency":"JPY"},"targetAmount":{"value":12.345,"currency":"KWD"},"rate":0.010004}
        }]""")

        fetcher.fetch(integration.id!!)

        val record = aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle()
        record.amountInCents.shouldBe(4200)
        record.counterpartyName.shouldBe("Philip J. Fry")
        record.counterpartyAccount.shouldBe("Planet Express account")
        record.sourceAmountInCents.shouldBe(1234)
        record.sourceCurrency.shouldBe("JPY")
        record.targetAmountInCents.shouldBe(12345)
        record.targetCurrency.shouldBe("KWD")
        record.exchangeRate.shouldBe(BigDecimal("0.010004"))
        record.feesInCents.shouldBe(null)
        record.runningBalanceInCents.shouldBe(null)
    }

    @Test
    fun `should checkpoint empty statements and backfill newly selected accounts independently`() {
        val previous = MOCK_TIME.minus(Duration.ofDays(1))
        val integration = preconditions { wiseIntegrationSettings(
            token = WiseApiMocks.TOKEN, accounts = setOf(dollars.copy(lastSuccessfulFetchTime = previous), euros),
            lastSuccessfulFetchTime = previous,
        ) }
        WiseApiMocks.setupStatement()
        WiseApiMocks.setupStatement(accountId = 302)

        fetcher.fetch(integration.id!!)

        assertInterval(start = "1999-03-20T23:01:02.042Z", end = MOCK_TIME.toString())
        assertInterval(start = "1998-12-28T23:01:02.042Z", end = MOCK_TIME.toString(), accountId = 302)
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        saved.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
        saved.accounts.all { it.lastSuccessfulFetchTime == MOCK_TIME }.shouldBe(true)
    }

    @Test
    fun `should split long gaps into supported statement windows`() {
        val integration = preconditions { wiseIntegrationSettings(
            token = WiseApiMocks.TOKEN, accounts = setOf(dollars.copy(lastSuccessfulFetchTime = Instant.parse("1997-03-28T23:01:02.042Z"))),
        ) }
        WiseApiMocks.setupStatement()
        fetcher.fetch(integration.id!!)
        assertInterval(start = "1997-03-21T23:01:02.042Z", end = "1998-03-21T23:01:02.042Z")
        assertInterval(start = "1998-03-21T23:01:02.042Z", end = "1999-03-21T23:01:02.042Z")
        assertInterval(start = "1999-03-21T23:01:02.042Z", end = MOCK_TIME.toString())
    }

    @Test
    fun `should backfill calendar months across month ends and leap days`() {
        for ((end, start) in listOf(
            "1999-05-31T12:00:00Z" to "1999-02-28T12:00:00Z",
            "1996-05-31T12:00:00Z" to "1996-02-29T12:00:00Z",
        )) {
            val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
            doReturn(Instant.parse(end)).whenever(time).currentTime()
            WiseApiMocks.setupStatement()

            fetcher.fetch(integration.id!!)

            assertInterval(start = start, end = end)
            val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().single { it.id == integration.id }
            saved.lastSuccessfulFetchTime.shouldBe(Instant.parse(end))
            saved.accounts.single().lastSuccessfulFetchTime.shouldBe(Instant.parse(end))
        }
    }

    @Test
    fun `should retain checkpoints and discard earlier windows when a later statement window fails`() {
        val previous = Instant.parse("1997-03-28T23:01:02.042Z")
        val integration = preconditions { wiseIntegrationSettings(
            token = WiseApiMocks.TOKEN, accounts = setOf(dollars.copy(lastSuccessfulFetchTime = previous)),
            lastSuccessfulFetchTime = previous,
        ) }
        WiseApiMocks.setupStatement(transactions = "[${transaction()}]")
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo(statementPath()))
            .withQueryParam("intervalStart", equalTo("1998-03-21T23:01:02.042Z"))
            .willReturn(aResponse().withStatus(503)))

        shouldThrow<WiseUnavailableException> { fetcher.fetch(integration.id!!) }

        assertInterval(start = "1997-03-21T23:01:02.042Z", end = "1998-03-21T23:01:02.042Z")
        assertInterval(start = "1998-03-21T23:01:02.042Z", end = "1999-03-21T23:01:02.042Z")
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        saved.lastSuccessfulFetchTime.shouldBe(previous)
        saved.accounts.single().lastSuccessfulFetchTime.shouldBe(previous)
        saved.authenticationRequired.shouldBe(false)
    }

    @Test
    fun `should not import partial results or advance checkpoints on transient failure`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars, euros)) }
        WiseApiMocks.setupStatement(transactions = "[${transaction()}]")
        WiseApiMocks.failStatement(accountId = 302)

        shouldThrow<WiseUnavailableException> { fetcher.fetch(integration.id!!) }

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        saved.lastSuccessfulFetchTime.shouldBe(null)
        saved.accounts.all { it.lastSuccessfulFetchTime == null }.shouldBe(true)
        saved.authenticationRequired.shouldBe(false)
    }

    @Test
    fun `should retain existing records and checkpoints after incremental account failures`() {
        val previous = MOCK_TIME.minus(Duration.ofDays(1))
        for (status in listOf(503, 401, 403)) {
            val integration = preconditions { wiseIntegrationSettings(
                token = WiseApiMocks.TOKEN,
                accounts = setOf(dollars.copy(lastSuccessfulFetchTime = previous), euros.copy(lastSuccessfulFetchTime = previous)),
                lastSuccessfulFetchTime = previous,
            ) }
            WiseApiMocks.setupStatement(transactions = "[${transaction()}]")
            WiseApiMocks.setupStatement(accountId = 302)
            doReturn(previous).whenever(time).currentTime()
            fetcher.fetch(integration.id!!)
            doReturn(MOCK_TIME).whenever(time).currentTime()
            val original = aggregateTemplate.findAll<AccountStatementRecord>().single { it.integrationId == integration.id }
            WiseApiMocks.setupStatement(transactions = "[${transaction()},${transaction(reference = "TRANSFER-42")}]")
            WiseApiMocks.failStatement(accountId = 302, status = status)

            if (status == 503) shouldThrow<WiseUnavailableException> { fetcher.fetch(integration.id) }
            else fetcher.fetch(integration.id)

            val savedRecords = aggregateTemplate.findAll<AccountStatementRecord>().filter { it.integrationId == integration.id }
            savedRecords.size.shouldBe(1)
            savedRecords.single().id.shouldBe(original.id)
            savedRecords.single().externalId.shouldBe("CARD-1")
            savedRecords.single().amountInCents.shouldBe(-125)
            val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().single { it.id == integration.id }
            saved.lastSuccessfulFetchTime.shouldBe(previous)
            saved.accounts.map { it.lastSuccessfulFetchTime }.shouldBe(listOf(previous, previous))
            saved.authenticationRequired.shouldBe(status != 503)
        }
    }

    @Test
    fun `should suspend unauthorized integrations without partial imports or checkpoint advancement`() {
        for (status in listOf(401, 403)) {
            val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
            WiseApiMocks.failStatement(status = status)

            fetcher.fetch(integration.id!!)

            val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().single { it.id == integration.id }
            saved.authenticationRequired.shouldBe(true)
            saved.lastSuccessfulFetchTime.shouldBe(null)
            saved.accounts.single().lastSuccessfulFetchTime.shouldBe(null)
        }
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        ThirdPartyApisMocks.server.resetRequests()
        job.fetchStatements()
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should skip disabled and authentication required integrations and continue after provider failures`() {
        preconditions {
            wiseIntegrationSettings(fetchTransactionsPeriodically = false, accounts = setOf(dollars))
            wiseIntegrationSettings(authenticationRequired = true, accounts = setOf(dollars))
            wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(euros))
            wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
        }
        WiseApiMocks.failStatement(accountId = 302)
        WiseApiMocks.setupStatement(transactions = "[${transaction()}]")

        job.fetchStatements()

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().amountInCents.shouldBe(-125)
        aggregateTemplate.findAll<WiseIntegrationSettings>().count { it.lastSuccessfulFetchTime == MOCK_TIME }.shouldBe(1)
        ThirdPartyApisMocks.server.verify(1, getRequestedFor(urlPathEqualTo(statementPath())))
    }

    @Test
    fun `should reject malformed statements without advancing checkpoints`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        for (invalid in listOf(
            "{}", transaction(amount = "0.001"), transaction(amount = "9223372036854775808"),
            transaction(reference = ""), transaction().replace(oldValue = "1999-03-27T12:00:00Z", newValue = "Slurm"),
            transaction().replace(oldValue = "USD", newValue = "EUR"),
        )) {
            WiseApiMocks.setupStatement(transactions = "[$invalid]")
            shouldThrow<WiseUnavailableException> { fetcher.fetch(integration.id!!) }
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
            aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().lastSuccessfulFetchTime.shouldBe(null)
        }
    }

    @Test
    fun `should preserve checkpoints and saved selections when changing settings and resume authentication`() {
        val data = preconditions {
            object {
                val fry = fry()
                val settings = wiseIntegrationSettings(
                    workspace = workspace(owner = fry), token = WiseApiMocks.TOKEN, accounts = setOf(dollars.copy(lastSuccessfulFetchTime = MOCK_TIME)),
                    authenticationRequired = true, lastSuccessfulFetchTime = MOCK_TIME,
                )
            }
        }
        WiseApiMocks.setupAccounts()
        runAs(data.fry.toSecurityPrincipal()) {
            service.loadAccounts(workspaceId = data.settings.workspaceId, token = null).single { it.account.currency == "USD" }.selected.shouldBe(true)
            service.setup(workspaceId = data.settings.workspaceId, token = null, accounts = listOf(
                WiseAccountSelection(profileId = "101", accountId = "301", currency = "USD"),
                WiseAccountSelection(profileId = "101", accountId = "302", currency = "EUR"),
            ), fetchTransactionsPeriodically = false)
        }
        val saved = aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
        saved.accounts.single { it.currency == "USD" }.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
        saved.accounts.single { it.currency == "EUR" }.lastSuccessfulFetchTime.shouldBe(null)
        saved.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
        saved.authenticationRequired.shouldBe(false)
        saved.fetchTransactionsPeriodically.shouldBe(false)
    }

    private fun transaction(reference: String = "CARD-1", direction: String = "DEBIT", amount: String = "-1.25") =
        """{"type":"$direction","date":"1999-03-27T12:00:00Z","referenceNumber":"$reference","amount":{"value":$amount,"currency":"USD"}}"""

    private fun statementPath(accountId: Long = 301) = "/wise/2026Q4/profiles/101/balance-statements/$accountId/statement.json"

    private fun assertInterval(start: String, end: String, accountId: Long = 301) {
        ThirdPartyApisMocks.server.verify(getRequestedFor(urlPathEqualTo(statementPath(accountId)))
            .withQueryParam("intervalStart", equalTo(start)).withQueryParam("intervalEnd", equalTo(end))
            .withQueryParam("currency", equalTo(if (accountId == 302L) "EUR" else "USD")))
    }
}
