package io.orangebuffalo.simpleaccounting.business.integrations

import io.kotest.assertions.throwables.shouldThrow
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class AccountStatementProcessingServiceTest(
    @Autowired private val service: AccountStatementProcessingService,
) : SaIntegrationTestBase() {
    @ParameterizedTest
    @CsvSource("en,Payment for Invoice #PE-42", "uk,Платіж за рахунком №PE-42")
    fun `should create invoice payment with transaction date tax and statement attachments once`(language: String, title: String) {
        val setup = preconditions {
            object {
                private val workspace = workspace(owner = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "en_AU", language = language)), defaultCurrency = "EUR")
                val integration = wiseIntegrationSettings(workspace = workspace)
                val document = document(workspace = workspace)
                val tax = generalTax(workspace = workspace, rateInBps = 2000)
                val invoice = invoice(
                    customer = customer(workspace = workspace), invoiceNumber = "PE-42", currency = "USD", amount = 12000,
                    generalTax = tax, status = InvoiceStatus.SENT, dateSent = LocalDate.parse("1999-03-20"),
                )
                val statement = accountStatementRecord(
                    integration = integration, type = StatementType.CREDIT, currency = "USD", amountInCents = 12000,
                    additionalData = "Payment PE-42", occurredAt = Instant.parse("1999-03-27T23:59:00Z"), attachments = setOf(document),
                )
            }
        }

        process(setup.integration)
        process(setup.integration)

        val income = aggregateTemplate.findAll<Income>().shouldBeSingle()
        income.shouldBeEntityWithFields(Income(
            workspaceId = setup.integration.workspaceId, categoryId = null, title = title,
            dateReceived = LocalDate.parse("1999-03-27"), currency = "USD", originalAmount = 12000,
            convertedAmounts = AmountsInDefaultCurrency(null), incomeTaxableAmounts = AmountsInDefaultCurrency(null),
            useDifferentExchangeRateForIncomeTaxPurposes = false, generalTaxId = setup.tax.id, generalTaxRateInBps = 2000,
            status = IncomeStatus.PENDING_CONVERSION, linkedInvoiceId = setup.invoice.id,
            attachments = setOf(IncomeAttachment(setup.document.id!!)),
        ))
        aggregateTemplate.findById(setup.invoice.id!!, Invoice::class.java)!!.shouldBeEntityWithFields(
            setup.invoice.copy(datePaid = LocalDate.parse("1999-03-27"), status = InvoiceStatus.PAID),
        )
        aggregateTemplate.findById(setup.statement.id!!, AccountStatementRecord::class.java)!!.shouldBeEntityWithFields(
            setup.statement.copy(linkedInvoiceId = setup.invoice.id, linkedIncomeId = income.id),
        )
    }

    @Test
    fun `should finalize domestic income using the manual payment calculations`() {
        val integration = preconditions {
            val workspace = workspace(defaultCurrency = "USD")
            wiseIntegrationSettings(workspace = workspace).also { integration ->
                val invoice = invoice(customer = customer(workspace = workspace), amount = 12000, generalTax = generalTax(workspace = workspace, rateInBps = 2000))
                accountStatementRecord(integration = integration, type = StatementType.CREDIT, amountInCents = 12000, linkedInvoice = invoice)
            }
        }
        process(integration)

        val income = aggregateTemplate.findAll<Income>().shouldBeSingle()
        income.convertedAmounts.shouldBe(AmountsInDefaultCurrency(originalAmountInDefaultCurrency = 12000, adjustedAmountInDefaultCurrency = 10000))
        income.incomeTaxableAmounts.shouldBe(income.convertedAmounts)
        income.generalTaxAmount.shouldBe(2000)
        income.status.shouldBe(IncomeStatus.FINALIZED)
    }

    @ParameterizedTest
    @ValueSource(strings = ["paid-date", "paid-status", "cancelled", "existing-income", "unmatched", "debit", "other-integration", "disabled"])
    fun `should not create an income for ineligible statements or invoices`(scenario: String) {
        val setup = preconditions {
            object {
                private val workspace = workspace()
                val integration = wiseIntegrationSettings(workspace = workspace)
                val invoice = invoice(
                    customer = customer(workspace = workspace),
                    datePaid = if (scenario == "paid-date") LocalDate.parse("1999-03-26") else null,
                    status = when (scenario) { "paid-status" -> InvoiceStatus.PAID; "cancelled" -> InvoiceStatus.CANCELLED; else -> InvoiceStatus.SENT },
                )
                val existingIncome = if (scenario == "existing-income") income(workspace = workspace, linkedInvoice = invoice) else null
                val statement = accountStatementRecord(
                    integration = if (scenario == "other-integration") wiseIntegrationSettings() else integration,
                    type = if (scenario == "debit") StatementType.DEBIT else StatementType.CREDIT,
                    linkedInvoice = if (scenario == "unmatched" || scenario == "other-integration") null else invoice,
                )
            }
        }
        process(integration = setup.integration, create = scenario != "disabled")

        val actualIncomes = aggregateTemplate.findAll<Income>()
        actualIncomes.map { it.id }.shouldBe(listOfNotNull(setup.existingIncome?.id))
        setup.existingIncome?.let { actualIncomes.single().shouldBeEntityWithFields(it) }
        aggregateTemplate.findById(setup.invoice.id!!, Invoice::class.java)!!.shouldBeEntityWithFields(setup.invoice)
        aggregateTemplate.findById(setup.statement.id!!, AccountStatementRecord::class.java)!!.shouldBeEntityWithFields(setup.statement)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should convert only integration created income and preserve tax amounts notes and attachments`(differentTaxRate: Boolean) {
        val setup = preconditions {
            object {
                private val workspace = workspace(defaultCurrency = "EUR")
                val integration = wiseIntegrationSettings(workspace = workspace)
                val originalDocument = document(workspace = workspace)
                val conversionDocument = document(workspace = workspace)
                val income = income(
                    workspace = workspace, currency = "USD", originalAmount = 12000, dateReceived = LocalDate.parse("1999-03-27"),
                    title = "Slurm delivery", convertedAmounts = AmountsInDefaultCurrency(null),
                    incomeTaxableAmounts = AmountsInDefaultCurrency(15000), useDifferentExchangeRateForIncomeTaxPurposes = differentTaxRate,
                    generalTax = generalTax(workspace = workspace, rateInBps = 2000),
                    attachments = setOf(originalDocument), notes = "Good news, everyone!", status = IncomeStatus.PENDING_CONVERSION,
                )
                init {
                    accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedIncome = income, accountId = "301")
                    income(workspace = workspace, currency = "USD", originalAmount = 12000, convertedAmounts = AmountsInDefaultCurrency(null))
                }
                val conversion = accountStatementRecord(
                    integration = integration, type = StatementType.CONVERSION, accountId = "302", occurredAt = Instant.parse("1999-03-27T00:00:00Z"),
                    sourceAmountInCents = 12000, sourceCurrency = "USD", targetAmountInCents = 10800, targetCurrency = "EUR",
                    exchangeRate = BigDecimal("0.9000"), attachments = setOf(originalDocument, conversionDocument),
                )
            }
        }
        process(integration = setup.integration, create = false)
        process(integration = setup.integration, create = false)

        aggregateTemplate.findById(setup.income.id!!, Income::class.java)!!.shouldBeEntityWithFields(setup.income.copy(
            convertedAmounts = AmountsInDefaultCurrency(originalAmountInDefaultCurrency = 10800, adjustedAmountInDefaultCurrency = 9000),
            incomeTaxableAmounts = if (differentTaxRate) AmountsInDefaultCurrency(originalAmountInDefaultCurrency = 15000, adjustedAmountInDefaultCurrency = 12500)
                else AmountsInDefaultCurrency(originalAmountInDefaultCurrency = 10800, adjustedAmountInDefaultCurrency = 9000),
            generalTaxRateInBps = 2000, generalTaxAmount = if (differentTaxRate) 2500 else 1800,
            status = IncomeStatus.FINALIZED,
            attachments = setOf(IncomeAttachment(setup.originalDocument.id!!), IncomeAttachment(setup.conversionDocument.id!!)),
            notes = "Good news, everyone!\nConverted on 1999-03-27: 1 USD = 0.9 EUR",
        ))
        aggregateTemplate.findById(setup.conversion.id!!, AccountStatementRecord::class.java)!!.shouldBeEntityWithFields(
            setup.conversion.copy(linkedIncomeId = setup.income.id),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ambiguous", "other-integration", "other-workspace", "manual", "actual-set", "source-currency", "target-currency", "amount", "before-receipt", "missing-source", "missing-target", "zero-source", "negative-target", "disabled", "linked"])
    fun `should leave incomes and conversions unchanged when conversion is ineligible`(scenario: String) {
        val setup = preconditions {
            object {
                private val workspace = workspace(defaultCurrency = "EUR")
                val integration = wiseIntegrationSettings(workspace = workspace)
                val income = income(
                    workspace = if (scenario == "other-workspace") workspace() else workspace,
                    currency = "USD", originalAmount = 12000, dateReceived = LocalDate.parse("1999-03-27"),
                    convertedAmounts = AmountsInDefaultCurrency(if (scenario == "actual-set") 10800 else null),
                )
                init {
                    if (scenario != "manual") accountStatementRecord(
                        integration = if (scenario == "other-integration") wiseIntegrationSettings() else integration,
                        type = StatementType.CREDIT, linkedIncome = income,
                    )
                    if (scenario == "ambiguous") {
                        val otherIncome = income(workspace = workspace, currency = "USD", originalAmount = 12000, dateReceived = LocalDate.parse("1999-03-27"), convertedAmounts = AmountsInDefaultCurrency(null))
                        accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedIncome = otherIncome)
                    }
                }
                val conversion = accountStatementRecord(
                    integration = integration, type = StatementType.CONVERSION,
                    occurredAt = Instant.parse(if (scenario == "before-receipt") "1999-03-26T23:59:59Z" else "1999-03-28T12:00:00Z"),
                    sourceAmountInCents = when (scenario) { "amount" -> 12001; "missing-source" -> null; "zero-source" -> 0; else -> 12000 },
                    sourceCurrency = if (scenario == "source-currency") "GBP" else "USD",
                    targetAmountInCents = when (scenario) { "missing-target" -> null; "negative-target" -> -1; else -> 10800 },
                    targetCurrency = if (scenario == "target-currency") "GBP" else "EUR",
                    linkedIncome = if (scenario == "linked") income else null,
                )
            }
        }
        val originalIncomes = aggregateTemplate.findAll<Income>().sortedBy { it.id }
        process(integration = setup.integration, create = false, convert = scenario != "disabled")

        val actualIncomes = aggregateTemplate.findAll<Income>().sortedBy { it.id }
        actualIncomes.map { it.id }.shouldBe(originalIncomes.map { it.id })
        actualIncomes.zip(originalIncomes).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
        aggregateTemplate.findById(setup.conversion.id!!, AccountStatementRecord::class.java)!!.shouldBeEntityWithFields(setup.conversion)
    }

    @Test
    fun `should create and convert in ordered passes even when conversion sorts before the credit`() {
        val integration = preconditions {
            val workspace = workspace(defaultCurrency = "EUR")
            wiseIntegrationSettings(workspace = workspace).also { integration ->
                val invoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-42", currency = "USD", amount = 12000)
                accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedInvoice = invoice, occurredAt = Instant.parse("1999-03-27T23:00:00Z"))
                accountStatementRecord(
                    integration = integration, type = StatementType.CONVERSION, occurredAt = Instant.parse("1999-03-27T01:00:00Z"),
                    sourceAmountInCents = 12000, sourceCurrency = "USD", targetAmountInCents = 10800, targetCurrency = "EUR",
                )
            }
        }
        process(integration)

        val income = aggregateTemplate.findAll<Income>().shouldBeSingle()
        income.convertedAmounts.shouldBe(AmountsInDefaultCurrency(10800))
        income.notes.shouldBe("Converted on 1999-03-27: 1 USD = 0.9 EUR")
        aggregateTemplate.findAll<AccountStatementRecord>().map { it.linkedIncomeId }.shouldBe(listOf(income.id, income.id))
    }

    @Test
    fun `should roll back income invoice and statement changes when a later income fails validation`() {
        val integration = preconditions {
            val workspace = workspace()
            wiseIntegrationSettings(workspace = workspace).also { integration ->
                val firstInvoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-42")
                val secondInvoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-43")
                accountStatementRecord(
                    integration = integration, type = StatementType.CREDIT, linkedInvoice = firstInvoice,
                    occurredAt = Instant.parse("1999-03-27T12:00:00Z"),
                )
                accountStatementRecord(
                    integration = integration, type = StatementType.CREDIT, linkedInvoice = secondInvoice,
                    occurredAt = Instant.parse("1999-03-27T13:00:00Z"), attachments = setOf(document()),
                )
            }
        }
        val originalInvoices = aggregateTemplate.findAll<Invoice>().sortedBy { it.id }
        val originalStatements = aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }

        shouldThrow<EntityNotFoundException> { process(integration) }

        aggregateTemplate.findAll<Income>().shouldBe(emptyList())
        val actualInvoices = aggregateTemplate.findAll<Invoice>().sortedBy { it.id }
        actualInvoices.map { it.id }.shouldBe(originalInvoices.map { it.id })
        actualInvoices.zip(originalInvoices).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
        val actualStatements = aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }
        actualStatements.map { it.id }.shouldBe(originalStatements.map { it.id })
        actualStatements.zip(originalStatements).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
    }

    private fun process(integration: WiseIntegrationSettings, create: Boolean = true, convert: Boolean = true) = service.process(
        workspaceId = integration.workspaceId, integrationId = integration.id!!,
        createIncomeFromInvoices = create, populateIncomeConvertedAmounts = convert,
    )
}
