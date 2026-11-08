package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesRepository
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesRepository
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.context.MessageSource
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.ZoneOffset
import java.util.Currency
import java.util.Locale

@Service
class AccountStatementProcessingService(
    private val records: AccountStatementRecordsRepository,
    private val dsl: DSLContext,
    private val incomes: IncomesRepository,
    private val incomesService: IncomesService,
    private val invoices: InvoicesRepository,
    private val workspaces: WorkspacesService,
    private val users: PlatformUsersService,
    private val messages: MessageSource,
) {
    @Transactional
    fun process(
        workspaceId: String,
        integrationId: String,
        createIncomeFromInvoices: Boolean,
        populateIncomeConvertedAmounts: Boolean,
        incomeCategoryId: String? = null,
    ): InvoiceMatchingResult {
        val matching = linkInvoices(workspaceId)
        val currentWorkspace = workspaces.getWorkspace(workspaceId)
        val owner = users.getUserByUserId(currentWorkspace.ownerId)
        // Invoice saves nested inside income persistence still require user authorization.
        runAs(owner.toSecurityPrincipal()) {
            if (createIncomeFromInvoices) createIncomes(
                workspaceId = workspaceId, integrationId = integrationId, language = owner.i18nSettings.language,
                categoryId = requireNotNull(incomeCategoryId) { "An income category is required for automatic income creation" },
            )
            if (populateIncomeConvertedAmounts) convertIncomes(
                workspaceId = workspaceId, integrationId = integrationId, defaultCurrency = currentWorkspace.defaultCurrency,
            )
        }
        return matching
    }

    private fun linkInvoices(workspaceId: String): InvoiceMatchingResult {
        val invoice = Tables.INVOICE
        val customer = Tables.CUSTOMER
        val statement = Tables.ACCOUNT_STATEMENT_RECORD
        val unlinked = records.findByWorkspaceIdAndLinkedInvoiceIdIsNullAndType(workspaceId = workspaceId, type = StatementType.CREDIT)
            .sortedWith(compareBy<AccountStatementRecord> { it.occurredAt }.thenBy { it.id })
        val links = mutableListOf<StatementInvoiceLink>()
        var ambiguous = 0
        val availableInvoices = dsl.select(invoice.id, invoice.invoiceNumber, invoice.currency, invoice.amount).from(invoice)
            .join(customer).on(customer.id.eq(invoice.customerId))
            .where(customer.workspaceId.eq(workspaceId))
            .andNotExists(DSL.selectOne().from(statement).where(statement.linkedInvoiceId.eq(invoice.id)))
            .fetch().associateBy { it[invoice.id]!! }.toMutableMap()
        unlinked.forEach { record ->
            val description = record.additionalData ?: return@forEach
            val candidates = availableInvoices.values.filter { candidate ->
                    val number = candidate[invoice.invoiceNumber]!!
                    candidate[invoice.currency] == record.currency && candidate[invoice.amount] == record.amountInCents &&
                        number.isNotBlank() && description.contains(other = number, ignoreCase = true)
                }
            if (candidates.size == 1) {
                val invoiceId = candidates.single()[invoice.id]!!
                records.save(record.copy(linkedInvoiceId = invoiceId))
                availableInvoices.remove(invoiceId)
                links.add(StatementInvoiceLink(statementId = record.id!!, invoiceId = invoiceId))
            } else if (candidates.size > 1) {
                ambiguous++
            }
        }
        return InvoiceMatchingResult(checked = unlinked.size, links = links, ambiguous = ambiguous)
    }

    private fun createIncomes(workspaceId: String, integrationId: String, language: String, categoryId: String) {
        val eligible = records.findForIncomeCreation(workspaceId = workspaceId, integrationId = integrationId)
        if (eligible.isEmpty()) return
        val invoicesById = invoices.findAllByIdsAndWorkspaceId(
            ids = eligible.map { it.linkedInvoiceId!! }, workspaceId = workspaceId,
        ).associateBy { it.id }
        eligible.forEach { record ->
            val invoiceId = record.linkedInvoiceId!!
            val invoice = invoicesById[invoiceId] ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
            val income = incomesService.saveIncome(Income(
                workspaceId = workspaceId,
                categoryId = categoryId,
                title = messages.getMessage("income.invoicePayment.title", arrayOf(invoice.invoiceNumber), Locale.forLanguageTag(language)),
                dateReceived = record.occurredAt.atZone(ZoneOffset.UTC).toLocalDate(),
                currency = invoice.currency,
                originalAmount = invoice.amount,
                convertedAmounts = AmountsInDefaultCurrency(null),
                incomeTaxableAmounts = AmountsInDefaultCurrency(null),
                useDifferentExchangeRateForIncomeTaxPurposes = false,
                generalTaxId = invoice.generalTaxId,
                status = IncomeStatus.PENDING_CONVERSION,
                linkedInvoiceId = invoice.id,
                attachments = record.attachments.map { IncomeAttachment(it.documentId) }.toSet(),
            ))
            records.save(record.copy(linkedIncomeId = income.id))
        }
    }

    private fun convertIncomes(workspaceId: String, integrationId: String, defaultCurrency: String) {
        val eligibleIncomes = incomes.findForStatementConversion(workspaceId = workspaceId, integrationId = integrationId)
            .associateBy { it.id }.toMutableMap()
        records.findForIncomeConversion(workspaceId = workspaceId, integrationId = integrationId, defaultCurrency = defaultCurrency).forEach { record ->
            val sourceAmount = record.sourceAmountInCents!!
            val targetAmount = record.targetAmountInCents!!
            val date = record.occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
            val candidates = eligibleIncomes.values.filter {
                it.currency == record.sourceCurrency && it.originalAmount == sourceAmount && !it.dateReceived.isAfter(date)
            }
            if (candidates.size != 1) return@forEach
            val matched = candidates.single()
            val rate = record.exchangeRate ?: targetAmount.toBigDecimal()
                .movePointLeft(Currency.getInstance(defaultCurrency).defaultFractionDigits)
                .divide(sourceAmount.toBigDecimal().movePointLeft(Currency.getInstance(matched.currency).defaultFractionDigits), 10, RoundingMode.HALF_UP)
            val note = "Converted on $date: 1 ${matched.currency} = ${rate.stripTrailingZeros().toPlainString()} $defaultCurrency"
            incomesService.saveIncome(matched.copy(
                convertedAmounts = AmountsInDefaultCurrency(targetAmount),
                attachments = matched.attachments + record.attachments.map { IncomeAttachment(it.documentId) },
                notes = listOfNotNull(matched.notes?.takeIf { it.isNotEmpty() }, note).joinToString("\n"),
            ))
            records.save(record.copy(linkedIncomeId = matched.id))
            eligibleIncomes.remove(matched.id)
        }
    }

    data class StatementInvoiceLink(val statementId: String, val invoiceId: String)

    data class InvoiceMatchingResult(val checked: Int = 0, val links: List<StatementInvoiceLink> = emptyList(), val ambiguous: Int = 0)
}
