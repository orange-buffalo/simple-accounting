package io.orangebuffalo.simpleaccounting.business.integrations

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.reload
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class AccountStatementProcessingServiceTest(
    @Autowired private val service: AccountStatementProcessingService,
    @Autowired private val messages: MessageSource,
) : SaIntegrationTestBase() {
    @Test
    fun `should create an invoice payment using the configured category and transaction details`() {
        val setup = preconditions { matchingInvoicePayment() }

        process(integration = setup.integration, create = true, convert = false)

        val income = aggregateTemplate.findSingle<Income>()
        income.shouldBeEntityWithFields(Income(
            workspaceId = setup.integration.workspaceId, categoryId = setup.category.id, title = "Payment for Invoice #PE-42",
            dateReceived = LocalDate.parse("1999-03-27"), currency = "USD", originalAmount = 12000,
            convertedAmounts = AmountsInDefaultCurrency(null), incomeTaxableAmounts = AmountsInDefaultCurrency(null),
            useDifferentExchangeRateForIncomeTaxPurposes = false, generalTaxId = setup.tax.id, generalTaxRateInBps = 2000,
            status = IncomeStatus.PENDING_CONVERSION, linkedInvoiceId = setup.invoice.id,
            attachments = setOf(IncomeAttachment(setup.document.id!!)),
        ))
        aggregateTemplate.reload(setup.invoice).shouldBeEntityWithFields(
            setup.invoice.copy(datePaid = LocalDate.parse("1999-03-27"), status = InvoiceStatus.PAID),
        )
        aggregateTemplate.reload(setup.statement).shouldBeEntityWithFields(
            setup.statement.copy(linkedIncomeId = income.id),
        )
    }

    @ParameterizedTest
    @CsvSource("en,Payment for Invoice #PE-42", "uk,Платіж за рахунком №PE-42", "fr,Payment for Invoice #PE-42")
    fun `should localize invoice payment titles with the message bundle`(language: String, expected: String) {
        messages.getMessage("income.invoicePayment.title", arrayOf("PE-42"), Locale.forLanguageTag(language)).shouldBe(expected)
    }

    @ParameterizedTest
    @CsvSource("en,Converted on 1999-03-27: 1 USD = 0.9 EUR", "uk,Конвертовано 1999-03-27: 1 USD = 0.9 EUR")
    fun `should localize conversion notes with the message bundle`(language: String, expected: String) {
        messages.getMessage("income.conversion.note", arrayOf("1999-03-27", "USD", "0.9", "EUR"), Locale.forLanguageTag(language))
            .shouldBe(expected)
    }

    @Test
    fun `should finalize domestic invoice income with inclusive tax`() {
        val integration = preconditions {
            val workspace = workspace(defaultCurrency = "USD")
            wiseIntegrationSettings(workspace = workspace, incomeCategory = category(workspace = workspace, income = true)).also { integration ->
                val invoice = invoice(
                    customer = customer(workspace = workspace), currency = "USD", amount = 12000,
                    generalTax = generalTax(workspace = workspace, rateInBps = 2000), datePaid = null, status = InvoiceStatus.SENT,
                )
                accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedInvoice = invoice, linkedIncome = null)
            }
        }
        process(integration = integration, create = true, convert = false)

        val income = aggregateTemplate.findSingle<Income>()
        income.convertedAmounts.shouldBe(AmountsInDefaultCurrency(originalAmountInDefaultCurrency = 12000, adjustedAmountInDefaultCurrency = 10000))
        income.incomeTaxableAmounts.shouldBe(income.convertedAmounts)
        income.generalTaxAmount.shouldBe(2000)
        income.status.shouldBe(IncomeStatus.FINALIZED)
    }

    @ParameterizedTest
    @CsvSource("SENT,1999-03-26", "PAID,null", "CANCELLED,null", nullValues = ["null"])
    fun `should skip paid or cancelled invoices`(status: InvoiceStatus, datePaid: LocalDate?) {
        val setup = preconditions { matchingInvoicePayment(invoiceStatus = status, invoiceDatePaid = datePaid) }
        process(integration = setup.integration, create = true, convert = false)
        aggregateTemplate.findAll<Income>().shouldBe(emptyList())
        aggregateTemplate.reload(setup.invoice).shouldBeEntityWithFields(setup.invoice)
        aggregateTemplate.reload(setup.statement).shouldBeEntityWithFields(setup.statement)
    }

    @Test
    fun `should skip invoices already linked to an income`() {
        val setup = preconditions {
            object {
                val payment = matchingInvoicePayment()
                val income = income(workspace = payment.workspace, linkedInvoice = payment.invoice)
            }
        }
        process(integration = setup.payment.integration, create = true, convert = false)
        aggregateTemplate.findSingle<Income>().shouldBeEntityWithFields(setup.income)
        aggregateTemplate.reload(setup.payment.statement).shouldBeEntityWithFields(setup.payment.statement)
    }

    @ParameterizedTest
    @CsvSource("CREDIT,false,true,true", "DEBIT,true,true,true", "CREDIT,true,false,true", "CREDIT,true,true,false")
    fun `should restrict income creation to eligible enabled integration statements`(
        type: StatementType, linkedInvoice: Boolean, sameIntegration: Boolean, enabled: Boolean,
    ) {
        val setup = preconditions {
            matchingInvoicePayment(statementType = type, linkInvoice = linkedInvoice, sameIntegration = sameIntegration,
                description = if (linkedInvoice) "Payment PE-42" else null)
        }
        process(integration = setup.integration, create = enabled, convert = false)
        aggregateTemplate.findAll<Income>().shouldBe(emptyList())
    }

    @ParameterizedTest
    @CsvSource("SENT,null", "PAID,1999-03-27", nullValues = ["null"])
    fun `should fail on a statement invoice outside its workspace`(status: InvoiceStatus, datePaid: LocalDate?) {
        val integration = preconditions {
            val workspace = workspace()
            wiseIntegrationSettings(workspace = workspace, incomeCategory = category(workspace = workspace, income = true)).also {
                accountStatementRecord(integration = it, type = StatementType.CREDIT, linkedInvoice = invoice(datePaid = datePaid, status = status))
            }
        }
        shouldThrow<EntityNotFoundException> { process(integration = integration, create = true, convert = false) }
        aggregateTemplate.findAll<Income>().shouldBe(emptyList())
    }

    @Test
    fun `should leave an already processed income statement and invoice unchanged`() {
        val setup = preconditions { matchingInvoicePayment() }
        process(integration = setup.integration, create = true, convert = false)
        val original = aggregateTemplate.findSingle<Income>()
        val statement = aggregateTemplate.findSingle<AccountStatementRecord>()
        val invoice = aggregateTemplate.findSingle<Invoice>()

        process(integration = setup.integration, create = true, convert = false)

        aggregateTemplate.findSingle<Income>().shouldBeEntityWithFields(original)
        aggregateTemplate.findSingle<AccountStatementRecord>().shouldBeEntityWithFields(statement)
        aggregateTemplate.findSingle<Invoice>().shouldBeEntityWithFields(invoice)
    }

    @ParameterizedTest
    @CsvSource("false,10800,9000,1800", "true,15000,12500,2500")
    fun `should populate conversion amounts preserving income data and distinct taxation`(
        differentTaxRate: Boolean, taxableAmount: Long, taxableAdjusted: Long, taxAmount: Long,
    ) {
        val setup = preconditions {
            object {
                private val workspace = workspace(owner = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "en_AU", language = "en")), defaultCurrency = "EUR")
                val integration = wiseIntegrationSettings(workspace = workspace)
                val originalDocument = document(workspace = workspace)
                val conversionDocument = document(workspace = workspace)
                private val invoice = invoice(customer = customer(workspace = workspace), datePaid = LocalDate.parse("1999-03-27"), status = InvoiceStatus.PAID)
                val income = income(
                    workspace = workspace, currency = "USD", originalAmount = 12000, dateReceived = LocalDate.parse("1999-03-27"),
                    title = "Slurm delivery", convertedAmounts = AmountsInDefaultCurrency(null), linkedInvoice = invoice,
                    incomeTaxableAmounts = AmountsInDefaultCurrency(15000), useDifferentExchangeRateForIncomeTaxPurposes = differentTaxRate,
                    generalTax = generalTax(workspace = workspace, rateInBps = 2000),
                    attachments = setOf(originalDocument), notes = "Good news, everyone!", status = IncomeStatus.PENDING_CONVERSION,
                )
                init { accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedInvoice = invoice, linkedIncome = null, accountId = "301") }
                val conversion = accountStatementRecord(
                    integration = integration, type = StatementType.CONVERSION, accountId = "302", occurredAt = Instant.parse("1999-03-27T00:00:00Z"), linkedIncome = null,
                    sourceAmountInCents = 12000, sourceCurrency = "USD", targetAmountInCents = 10800, targetCurrency = "EUR",
                    exchangeRate = BigDecimal("0.9000"), attachments = setOf(originalDocument, conversionDocument),
                )
            }
        }
        process(integration = setup.integration, create = false, convert = true)

        aggregateTemplate.reload(setup.income).shouldBeEntityWithFields(setup.income.copy(
            convertedAmounts = AmountsInDefaultCurrency(originalAmountInDefaultCurrency = 10800, adjustedAmountInDefaultCurrency = 9000),
            incomeTaxableAmounts = AmountsInDefaultCurrency(originalAmountInDefaultCurrency = taxableAmount, adjustedAmountInDefaultCurrency = taxableAdjusted),
            generalTaxRateInBps = 2000, generalTaxAmount = taxAmount, status = IncomeStatus.FINALIZED,
            attachments = setOf(IncomeAttachment(setup.originalDocument.id!!), IncomeAttachment(setup.conversionDocument.id!!)),
            notes = "Good news, everyone!\nConverted on 1999-03-27: 1 USD = 0.9 EUR",
        ))
        aggregateTemplate.reload(setup.conversion).shouldBeEntityWithFields(setup.conversion.copy(linkedIncomeId = setup.income.id))
    }

    @ParameterizedTest
    @EnumSource(StatementType::class)
    fun `should match invoice provenance without depending on statement type or income linkage`(type: StatementType) {
        val integration = preconditions {
            val workspace = workspace(defaultCurrency = "EUR")
            wiseIntegrationSettings(workspace = workspace).also { integration ->
                val invoice = invoice(customer = customer(workspace = workspace), datePaid = LocalDate.parse("1999-03-27"), status = InvoiceStatus.PAID)
                income(workspace = workspace, linkedInvoice = invoice, currency = "USD", originalAmount = 12000,
                    dateReceived = LocalDate.parse("1999-03-27"), convertedAmounts = AmountsInDefaultCurrency(null),
                    generalTax = null, useDifferentExchangeRateForIncomeTaxPurposes = false)
                accountStatementRecord(integration = integration, type = type, linkedInvoice = invoice, linkedIncome = null)
                accountStatementRecord(integration = integration, type = StatementType.CONVERSION, linkedIncome = null,
                    occurredAt = Instant.parse("1999-03-28T12:00:00Z"), sourceAmountInCents = 12000, sourceCurrency = "USD", targetAmountInCents = 10800, targetCurrency = "EUR", exchangeRate = BigDecimal("0.9"))
            }
        }
        process(integration = integration, create = false, convert = true)
        aggregateTemplate.findSingle<Income>().convertedAmounts.shouldBe(AmountsInDefaultCurrency(10800))
    }

    @ParameterizedTest
    @CsvSource(
        "12000,GBP,10800,EUR,1999-03-28T12:00:00Z,null",
        "12000,USD,10800,GBP,1999-03-28T12:00:00Z,null",
        "12001,USD,10800,EUR,1999-03-28T12:00:00Z,null",
        "12000,USD,10800,EUR,1999-03-26T23:59:59Z,null",
        "null,USD,10800,EUR,1999-03-28T12:00:00Z,null",
        "0,USD,10800,EUR,1999-03-28T12:00:00Z,null",
        "12000,USD,null,EUR,1999-03-28T12:00:00Z,null",
        "12000,USD,-1,EUR,1999-03-28T12:00:00Z,null",
        "12000,USD,10800,EUR,1999-03-28T12:00:00Z,10800",
        nullValues = ["null"],
    )
    fun `should leave income unchanged for ineligible conversion values`(
        sourceAmount: Long?, sourceCurrency: String, targetAmount: Long?, targetCurrency: String, occurredAt: Instant, actualAmount: Long?,
    ) {
        val setup = preconditions {
            matchingConversion(sourceAmount = sourceAmount, sourceCurrency = sourceCurrency, targetAmount = targetAmount,
                targetCurrency = targetCurrency, occurredAt = occurredAt, actualAmount = actualAmount)
        }
        process(integration = setup.integration, create = false, convert = true)
        aggregateTemplate.findSingle<Income>().shouldBeEntityWithFields(setup.income)
        aggregateTemplate.reload(setup.conversion).shouldBeEntityWithFields(setup.conversion)
    }

    @ParameterizedTest
    @CsvSource("0,true,true,false", "1,false,true,false", "1,true,false,false", "1,true,true,true", "2,true,true,false")
    fun `should skip disabled linked ambiguous or unrelated conversions`(
        matchingInvoices: Int, sameIntegration: Boolean, enabled: Boolean, alreadyLinked: Boolean,
    ) {
        val setup = preconditions {
            matchingConversion(matchingInvoices = matchingInvoices, sameIntegration = sameIntegration, alreadyLinked = alreadyLinked)
        }
        val originalIncomes = aggregateTemplate.findAll<Income>().sortedBy { it.id }
        val originalStatements = aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }
        process(integration = setup.integration, create = false, convert = enabled)
        aggregateTemplate.findAll<Income>().sortedBy { it.id }.map { it.id }.shouldBe(originalIncomes.map { it.id })
        aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }.map { it.id }.shouldBe(originalStatements.map { it.id })
        aggregateTemplate.findAll<Income>().sortedBy { it.id }.zip(originalIncomes).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
        aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }.zip(originalStatements).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
    }

    @Test
    fun `should create and convert in ordered passes even when conversion sorts before the credit`() {
        val integration = preconditions {
            val workspace = workspace(owner = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "en_AU", language = "en")), defaultCurrency = "EUR")
            wiseIntegrationSettings(workspace = workspace, incomeCategory = category(workspace = workspace, income = true)).also { integration ->
                val invoice = invoice(customer = customer(workspace = workspace), invoiceNumber = "PE-42", currency = "USD", amount = 12000, datePaid = null, status = InvoiceStatus.SENT, generalTax = null)
                accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedInvoice = invoice, linkedIncome = null, occurredAt = Instant.parse("1999-03-27T23:00:00Z"))
                accountStatementRecord(integration = integration, type = StatementType.CONVERSION, linkedIncome = null, occurredAt = Instant.parse("1999-03-27T01:00:00Z"),
                    sourceAmountInCents = 12000, sourceCurrency = "USD", targetAmountInCents = 10800, targetCurrency = "EUR", exchangeRate = null)
            }
        }
        process(integration = integration, create = true, convert = true)
        val income = aggregateTemplate.findSingle<Income>()
        income.convertedAmounts.shouldBe(AmountsInDefaultCurrency(10800))
        income.notes.shouldBe("Converted on 1999-03-27: 1 USD = 0.9 EUR")
        aggregateTemplate.findAll<AccountStatementRecord>().map { it.linkedIncomeId }.shouldBe(listOf(income.id, income.id))
    }

    @Test
    fun `should roll back invoice payment processing when a document fails validation`() {
        val setup = preconditions {
            matchingInvoicePayment().also { payment ->
                accountStatementRecord(integration = payment.integration, type = StatementType.CONVERSION, linkedIncome = null,
                    occurredAt = Instant.parse("1999-03-28T12:00:00Z"), sourceAmountInCents = 12000, sourceCurrency = "USD",
                    targetAmountInCents = 10800, targetCurrency = "EUR", exchangeRate = BigDecimal("0.9"), attachments = setOf(document()))
            }
        }
        val originalInvoices = aggregateTemplate.findAll<Invoice>().sortedBy { it.id }
        val originalStatements = aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }
        shouldThrow<EntityNotFoundException> { process(integration = setup.integration, create = true, convert = true) }
        aggregateTemplate.findAll<Income>().shouldBe(emptyList())
        val actualInvoices = aggregateTemplate.findAll<Invoice>().sortedBy { it.id }
        actualInvoices.map { it.id }.shouldBe(originalInvoices.map { it.id })
        actualInvoices.zip(originalInvoices).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
        val actualStatements = aggregateTemplate.findAll<AccountStatementRecord>().sortedBy { it.id }
        actualStatements.map { it.id }.shouldBe(originalStatements.map { it.id })
        actualStatements.zip(originalStatements).forEach { (actual, expected) -> actual.shouldBeEntityWithFields(expected) }
    }

    private fun process(integration: WiseIntegrationSettings, create: Boolean, convert: Boolean) = service.process(
        workspaceId = integration.workspaceId, integrationId = integration.id!!,
        createIncomeFromInvoices = create, populateIncomeConvertedAmounts = convert, incomeCategoryId = integration.incomeCategoryId,
    )

    @Test
    fun `should convert a fully matching invoice income`() {
        val setup = preconditions { matchingConversion() }
        process(integration = setup.integration, create = false, convert = true)
        aggregateTemplate.reload(setup.income).convertedAmounts.shouldBe(AmountsInDefaultCurrency(10800))
        aggregateTemplate.reload(setup.conversion).linkedIncomeId.shouldBe(setup.income.id)
    }

    @Test
    fun `should preserve maximum legacy notes when appending a conversion explanation`() {
        val notes = "Slurm".repeat(204) + "Moon"
        val setup = preconditions { matchingConversion(notes = notes) }
        process(integration = setup.integration, create = false, convert = true)
        aggregateTemplate.reload(setup.income).notes.shouldBe("$notes\nConverted on 1999-03-28: 1 USD = 0.9 EUR")
    }

    private fun EntitiesFactory.matchingConversion(
        sourceAmount: Long? = 12000,
        sourceCurrency: String = "USD",
        targetAmount: Long? = 10800,
        targetCurrency: String = "EUR",
        occurredAt: Instant = Instant.parse("1999-03-28T12:00:00Z"),
        actualAmount: Long? = null,
        matchingInvoices: Int = 1,
        sameIntegration: Boolean = true,
        alreadyLinked: Boolean = false,
        notes: String? = null,
    ): MatchingConversion {
        val workspace = workspace(owner = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "en_AU", language = "en")), defaultCurrency = "EUR")
        val integration = wiseIntegrationSettings(workspace = workspace)
        val invoice = invoice(customer = customer(workspace = workspace), datePaid = LocalDate.parse("1999-03-27"), status = InvoiceStatus.PAID)
        val income = income(workspace = workspace, linkedInvoice = invoice, currency = "USD", originalAmount = 12000,
            dateReceived = LocalDate.parse("1999-03-27"), convertedAmounts = AmountsInDefaultCurrency(actualAmount),
            generalTax = null, useDifferentExchangeRateForIncomeTaxPurposes = false, notes = notes)
        if (matchingInvoices > 0) {
            accountStatementRecord(integration = if (sameIntegration) integration else wiseIntegrationSettings(), type = StatementType.CREDIT, linkedInvoice = invoice, linkedIncome = null)
        }
        repeat((matchingInvoices - 1).coerceAtLeast(0)) {
            val otherInvoice = invoice(customer = customer(workspace = workspace), datePaid = LocalDate.parse("1999-03-27"), status = InvoiceStatus.PAID)
            income(workspace = workspace, linkedInvoice = otherInvoice, currency = "USD", originalAmount = 12000,
                dateReceived = LocalDate.parse("1999-03-27"), convertedAmounts = AmountsInDefaultCurrency(null), generalTax = null,
                useDifferentExchangeRateForIncomeTaxPurposes = false)
            accountStatementRecord(integration = integration, type = StatementType.CREDIT, linkedInvoice = otherInvoice, linkedIncome = null)
        }
        val conversion = accountStatementRecord(integration = integration, type = StatementType.CONVERSION, linkedIncome = income.takeIf { alreadyLinked },
            occurredAt = occurredAt, sourceAmountInCents = sourceAmount, sourceCurrency = sourceCurrency,
            targetAmountInCents = targetAmount, targetCurrency = targetCurrency, exchangeRate = BigDecimal("0.9"))
        return MatchingConversion(integration = integration, income = income, conversion = conversion)
    }

    private data class MatchingConversion(val integration: WiseIntegrationSettings, val income: Income, val conversion: AccountStatementRecord)

    private fun EntitiesFactory.matchingInvoicePayment(
        invoiceStatus: InvoiceStatus = InvoiceStatus.SENT,
        invoiceDatePaid: LocalDate? = null,
        invoiceCurrency: String = "USD",
        invoiceAmount: Long = 12000,
        statementType: StatementType = StatementType.CREDIT,
        statementCurrency: String = "USD",
        statementAmount: Long = 12000,
        description: String? = "Payment PE-42",
        linkInvoice: Boolean = true,
        sameIntegration: Boolean = true,
    ): InvoicePayment {
        val owner = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "en_AU", language = "en"))
        val workspace = workspace(owner = owner, defaultCurrency = "EUR")
        val category = category(workspace = workspace, income = true)
        val integration = wiseIntegrationSettings(workspace = workspace, createIncomeFromInvoices = true, incomeCategory = category)
        val document = document(workspace = workspace)
        val tax = generalTax(workspace = workspace, rateInBps = 2000)
        val invoice = invoice(
            customer = customer(workspace = workspace), invoiceNumber = "PE-42", currency = invoiceCurrency, amount = invoiceAmount,
            generalTax = tax, status = invoiceStatus, dateSent = LocalDate.parse("1999-03-20"), datePaid = invoiceDatePaid,
        )
        val statement = accountStatementRecord(
            integration = if (sameIntegration) integration else wiseIntegrationSettings(), type = statementType,
            currency = statementCurrency, amountInCents = statementAmount, additionalData = description,
            occurredAt = Instant.parse("1999-03-27T23:59:00Z"), attachments = setOf(document),
            linkedInvoice = invoice.takeIf { linkInvoice }, linkedIncome = null,
        )
        return InvoicePayment(workspace = workspace, category = category, integration = integration,
            document = document, tax = tax, invoice = invoice, statement = statement)
    }

    private data class InvoicePayment(
        val workspace: Workspace,
        val category: Category,
        val integration: WiseIntegrationSettings,
        val document: Document,
        val tax: GeneralTax,
        val invoice: Invoice,
        val statement: AccountStatementRecord,
    )
}
