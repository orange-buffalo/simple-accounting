package io.orangebuffalo.simpleaccounting.business.integrations.wise

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.business.integrations.IntegrationProvider
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
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
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.NullSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Random

@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WiseStatementFetchServiceTest(
    @Autowired private val fetcher: WiseStatementFetchService,
    @Autowired private val service: WiseIntegrationService,
    @Autowired private val time: TimeService,
) : SaIntegrationTestBase() {
    private val dollars = WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")
    private val euros = WiseIntegrationAccount(profileId = 101, accountId = 302, currency = "EUR")

    @Nested
    inner class InvoiceLinking {
        @ParameterizedTest
        @CsvSource(value = ["paymentReference|PE-3025-42", "description|PE-3025-42", "paymentReference|PE%_.*[42]"], delimiter = '|')
        fun `should link newly imported credits using descriptive fields`(field: String, invoiceNumber: String) {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    private val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val invoice = invoice(customer = customer(workspace = workspace), invoiceNumber = invoiceNumber, amount = 125, currency = "USD")
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-43", amount = 125, currency = "USD")
                    }
                }
            }
            WiseApiMocks.setupStatement(transactions = """[{
                "type":"CREDIT","date":"1999-03-27T12:00:00Z","referenceNumber":"TRANSFER-42",
                "amount":{"value":1.25,"currency":"USD"},
                "details":{"$field":"Payment for ${invoiceNumber.lowercase()}, Planet Express"}
            }]""")

            fetcher.fetchStatements()

            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().linkedInvoiceId.shouldBe(data.invoice.id)
        }

        @ParameterizedTest
        @EnumSource(InvoiceStatus::class)
        fun `should revisit existing unlinked credits for any invoice status`(status: InvoiceStatus) {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(
                        integration = integration, type = StatementType.CREDIT,
                        amountInCents = 125, currency = "USD", additionalData = "Payment reference: PE-3025-42",
                    )
                    val invoice = invoice(
                        customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125, currency = "USD", status = status,
                    )
                }
            }
            WiseApiMocks.setupStatement()

            fetcher.fetch(data.integration.id!!)

            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle()
                .shouldBeEntityWithFields(data.record.copy(linkedInvoiceId = data.invoice.id))
        }

        @ParameterizedTest
        @ValueSource(strings = ["PE-3025-43", "PE-3025-42"])
        fun `should leave multiple matching invoices ambiguous`(otherNumber: String) {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(
                        integration = integration, type = StatementType.CREDIT,
                        additionalData = "Description: PE-3025-42 and PE-3025-43",
                    )
                    init {
                        val customer = customer(workspace = workspace)
                        invoice(customer = customer, invoiceNumber = "PE-3025-42", amount = 125)
                        invoice(customer = customer, invoiceNumber = otherNumber, amount = 125)
                    }
                }
            }
            WiseApiMocks.setupStatement()

            fetcher.fetch(data.integration.id!!)

            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["", "Description: Slurm delivery"])
        fun `should not match credits without a descriptive invoice number`(description: String?) {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(
                        integration = integration, type = StatementType.CREDIT, additionalData = description,
                        partyName = "PE-3025-42", externalId = "PE-3025-42",
                    )
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125)
                    }
                }
            }
            WiseApiMocks.setupStatement()

            fetcher.fetch(data.integration.id!!)

            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should not link invoices with a different amount`() {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(integration = integration, type = StatementType.CREDIT, amountInCents = 125, additionalData = "Payment reference: PE-3025-42")
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 126)
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should not link invoices with a different currency`() {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(integration = integration, type = StatementType.CREDIT, currency = "USD", additionalData = "Payment reference: PE-3025-42")
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125, currency = "EUR")
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should not link invoices from another workspace`() {
            val data = preconditions {
                object {
                    val integration = wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(integration = integration, type = StatementType.CREDIT, additionalData = "Payment reference: PE-3025-42")
                    init {
                        invoice(invoiceNumber = "PE-3025-42", amount = 125)
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should not link invoices with a different number`() {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(integration = integration, type = StatementType.CREDIT, additionalData = "Payment reference: PE-3025-42")
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-43", amount = 125)
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @ParameterizedTest
        @EnumSource(value = StatementType::class, names = ["DEBIT", "CONVERSION", "OTHER"])
        fun `should not link non-credit transactions`(type: StatementType) {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val record = accountStatementRecord(integration = integration, type = type, additionalData = "Description: PE-3025-42")
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125)
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should preserve an existing statement link`() {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    private val invoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125)
                    val record = accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedInvoice = invoice, additionalData = "Description: PE-3025-43")
                    init {
                        invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-43", amount = 125)
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should not reuse an invoice linked to another statement`() {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    private val invoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125)
                    private val linked = accountStatementRecord(integration = integration, linkedInvoice = invoice)
                    val record = accountStatementRecord(integration = integration, type = StatementType.CREDIT, additionalData = "Payment reference: PE-3025-42")
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            aggregateTemplate.findAll<AccountStatementRecord>().single { it.id == data.record.id }.shouldBeEntityWithFields(data.record)
        }

        @Test
        fun `should link an unused invoice to at most one statement`() {
            val data = preconditions {
                object {
                    private val workspace = workspace()
                    val integration = wiseIntegrationSettings(workspace = workspace, token = WiseApiMocks.TOKEN, accounts = setOf(dollars))
                    val invoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-3025-42", amount = 125)
                    init {
                        repeat(2) { accountStatementRecord(integration = integration, type = StatementType.CREDIT, additionalData = "Payment reference: PE-3025-42") }
                    }
                }
            }
            WiseApiMocks.setupStatement()
            fetcher.fetch(data.integration.id!!)
            val records = aggregateTemplate.findAll<AccountStatementRecord>()
            records.size.shouldBe(2)
            records.count { it.linkedInvoiceId == data.invoice.id }.shouldBe(1)
            records.count { it.linkedInvoiceId == null }.shouldBe(1)
        }
    }

    @Test
    fun `should import three calendar months and preserve generic transaction details`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        WiseApiMocks.setupStatement(transactions = """[{
            "type":"DEBIT","date":"1999-03-27T12:00:00Z","referenceNumber":"CARD-249281",
            "amount":{"value":-7.76,"currency":"USD"},"totalFees":{"value":0.04,"currency":"USD"},
            "runningBalance":{"value":-16.01,"currency":"USD"},
            "details":{"type":"CARD","description":"Slurm supplies","paymentReference":"Planet Express",
                "merchant":{"name":"Slurm factory","firstLine":"Robot street","city":"New New York","country":"US","category":"Slurm"}},
            "exchangeDetails":{"forAmount":{"value":6.8,"currency":"GBP"},"rate":1.147806},"extra":"ignored"
        }]""")

        fetcher.fetch(integration.id!!)

        assertInterval(start = "1998-12-28T23:01:02.042Z", end = MOCK_TIME.toString())
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(AccountStatementRecord(
            workspaceId = integration.workspaceId, provider = IntegrationProvider.WISE, integrationId = integration.id!!,
            accountId = "301", accountCurrency = "USD", externalId = """["101","USD","DEBIT","CARD-249281"]""",
            type = StatementType.DEBIT, occurredAt = Instant.parse("1999-03-27T12:00:00Z"), amountInCents = 776, currency = "USD",
            feesInCents = 4, feesCurrency = "USD", runningBalanceInCents = -1601, runningBalanceCurrency = "USD",
            additionalData = "Type: CARD\nDescription: Slurm supplies\nPayment reference: Planet Express\nMerchant: Slurm factory\nMerchant category: Slurm\nMerchant address: Robot street, New New York, US",
            partyName = "Slurm factory",
            originalAmountInCents = 680, originalCurrency = "GBP", exchangeRate = BigDecimal("1.147806"),
        ))
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
        records.single { it.type == StatementType.DEBIT && it.externalId == """["101","USD","DEBIT","CARD-1"]""" }.id.shouldBe(originalId)
        records.single { it.type == StatementType.CREDIT }.amountInCents.shouldBe(125)
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
        records.map { "${it.integrationId}:${it.accountId}:${it.externalId}" }.toSet().shouldBe(setOf(
            "${integrations[0].id}:301:[\"101\",\"USD\",\"DEBIT\",\"CARD-1\"]",
            "${integrations[0].id}:301:[\"101\",\"EUR\",\"DEBIT\",\"CARD-1\"]",
            "${integrations[0].id}:301:[\"202\",\"USD\",\"DEBIT\",\"CARD-1\"]",
            "${integrations[0].id}:303:[\"101\",\"USD\",\"DEBIT\",\"CARD-1\"]",
            "${integrations[1].id}:301:[\"101\",\"USD\",\"DEBIT\",\"CARD-1\"]",
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

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(AccountStatementRecord(
            workspaceId = integration.workspaceId, provider = IntegrationProvider.WISE, integrationId = integration.id!!,
            accountId = "301", accountCurrency = "USD", externalId = """["101","USD","CREDIT","TRANSFER-42"]""",
            type = StatementType.CONVERSION, occurredAt = Instant.parse("1999-03-27T12:00:00Z"), amountInCents = 4200, currency = "USD",
            partyName = "Philip J. Fry",
            additionalData = "Type: CONVERSION\nSender: Philip J. Fry\nSender account: Planet Express account",
            sourceAmountInCents = 1234, sourceCurrency = "JPY", targetAmountInCents = 12345, targetCurrency = "KWD",
            exchangeRate = BigDecimal("0.010004"),
        ))
    }

    @Test
    fun `should retain unsupported transaction activities as other with a positive amount`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        WiseApiMocks.setupStatement(transactions = "[${transaction(direction = "FUTURE_ACTIVITY")}]")

        fetcher.fetch(integration.id!!)

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().shouldBeEntityWithFields(AccountStatementRecord(
            workspaceId = integration.workspaceId, provider = IntegrationProvider.WISE, integrationId = integration.id,
            accountId = "301", accountCurrency = "USD", externalId = """["101","USD","FUTURE_ACTIVITY","CARD-1"]""",
            type = StatementType.OTHER, occurredAt = Instant.parse("1999-03-27T12:00:00Z"), amountInCents = 125, currency = "USD",
        ))
    }

    @Test
    fun `should roll back earlier inserts and preserve checkpoints after a database write failure`() {
        val previous = MOCK_TIME.minus(Duration.ofDays(1))
        val integration = preconditions { wiseIntegrationSettings(
            token = WiseApiMocks.TOKEN, accounts = setOf(dollars.copy(lastSuccessfulFetchTime = previous)),
            lastSuccessfulFetchTime = previous,
        ) }
        setupStatementWithOversizedReference()

        fetcher.fetch(integration.id!!)

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle().shouldBeEntityWithFields(integration)
    }

    @Test
    fun `should continue processing integrations after an unexpected persistence failure`() {
        preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        val valid = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(euros)) }
        setupStatementWithOversizedReference()
        WiseApiMocks.setupStatement(accountId = 302, transactions = "[${transaction().replace(oldValue = "USD", newValue = "EUR")}]")

        fetcher.fetchStatements()

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().integrationId.shouldBe(valid.id)
        aggregateTemplate.findAll<WiseIntegrationSettings>().single { it.id == valid.id }.lastSuccessfulFetchTime.shouldBe(MOCK_TIME)
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

        fetcher.fetch(integration.id!!)

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

        fetcher.fetch(integration.id!!)

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

            fetcher.fetch(integration.id)

            val savedRecords = aggregateTemplate.findAll<AccountStatementRecord>().filter { it.integrationId == integration.id }
            savedRecords.size.shouldBe(1)
            savedRecords.single().id.shouldBe(original.id)
            savedRecords.single().externalId.shouldBe("""["101","USD","DEBIT","CARD-1"]""")
            savedRecords.single().amountInCents.shouldBe(125)
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
        fetcher.fetchStatements()
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should suspend an integration challenged for SCA despite an empty response body`() {
        val previous = MOCK_TIME.minus(Duration.ofDays(1))
        val integration = preconditions { wiseIntegrationSettings(
            token = WiseApiMocks.TOKEN, accounts = setOf(dollars.copy(lastSuccessfulFetchTime = previous)),
            lastSuccessfulFetchTime = previous,
        ) }
        ThirdPartyApisMocks.server.stubFor(get(urlPathEqualTo(statementPath()))
            .willReturn(aResponse().withStatus(403)
                .withHeader("x-2fa-approval-result", "REJECTED")
                .withHeader("x-2fa-approval", "bender-sca-challenge")))

        fetcher.fetch(integration.id!!)

        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBeSingle()
            .shouldBeEntityWithFields(integration.copy(authenticationRequired = true))
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        ThirdPartyApisMocks.server.resetRequests()
        fetcher.fetch(integration.id)
        fetcher.fetchStatements()
        WiseApiMocks.shouldHaveNoRequests()
    }

    @Test
    fun `should ignore ineligible and missing integrations when fetching a single integration`() {
        val integrations = preconditions { listOf(
            wiseIntegrationSettings(fetchTransactionsPeriodically = false, accounts = setOf(dollars)),
            wiseIntegrationSettings(authenticationRequired = true, accounts = setOf(dollars)),
            wiseIntegrationSettings(accounts = emptySet()),
        ) }

        integrations.forEach { fetcher.fetch(it.id!!) }
        fetcher.fetch("bender")

        WiseApiMocks.shouldHaveNoRequests()
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
        val saved = aggregateTemplate.findAll<WiseIntegrationSettings>()
        integrations.forEach { expected -> saved.single { it.id == expected.id }.shouldBeEntityWithFields(expected) }
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

        fetcher.fetchStatements()

        aggregateTemplate.findAll<AccountStatementRecord>().shouldBeSingle().amountInCents.shouldBe(125)
        aggregateTemplate.findAll<WiseIntegrationSettings>().count { it.lastSuccessfulFetchTime == MOCK_TIME }.shouldBe(1)
        ThirdPartyApisMocks.server.verify(1, getRequestedFor(urlPathEqualTo(statementPath())))
    }

    @Test
    fun `should reject malformed statements without advancing checkpoints`() {
        val integration = preconditions { wiseIntegrationSettings(token = WiseApiMocks.TOKEN, accounts = setOf(dollars)) }
        for (invalid in listOf(
            "{}", transaction(amount = "0.001"), transaction(amount = "9223372036854775808"), transaction(amount = "-92233720368547758.08"),
            transaction(reference = ""), transaction().replace(oldValue = "1999-03-27T12:00:00Z", newValue = "Slurm"),
            transaction().replace(oldValue = "USD", newValue = "EUR"),
        )) {
            WiseApiMocks.setupStatement(transactions = "[$invalid]")
            fetcher.fetch(integration.id!!)
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

    private fun setupStatementWithOversizedReference() {
        // Incompressible provider data exceeds PostgreSQL's index-entry limit on the second insert.
        val bytes = ByteArray(16000).also { Random(101).nextBytes(it) }
        val reference = Base64.getEncoder().encodeToString(bytes)
        WiseApiMocks.setupStatement(transactions = "[${transaction()},${transaction(reference = reference)}]")
    }

    private fun statementPath(accountId: Long = 301) = "/wise/2026Q4/profiles/101/balance-statements/$accountId/statement.json"

    private fun assertInterval(start: String, end: String, accountId: Long = 301) {
        ThirdPartyApisMocks.server.verify(getRequestedFor(urlPathEqualTo(statementPath(accountId)))
            .withQueryParam("intervalStart", equalTo(start)).withQueryParam("intervalEnd", equalTo(end))
            .withQueryParam("currency", equalTo(if (accountId == 302L) "EUR" else "USD")))
    }
}
