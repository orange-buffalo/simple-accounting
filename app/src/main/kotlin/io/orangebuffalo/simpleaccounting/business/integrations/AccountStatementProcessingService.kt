package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesRepository
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesService
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.ZoneOffset
import java.util.Currency

@Service
class AccountStatementProcessingService(
    private val records: AccountStatementRecordsRepository,
    private val dsl: DSLContext,
    private val incomes: IncomesRepository,
    private val incomesService: IncomesService,
    private val invoices: InvoicesService,
    private val workspaces: WorkspacesService,
    private val users: PlatformUsersService,
) {
    @Transactional
    fun process(
        workspaceId: String,
        integrationId: String,
        createIncomeFromInvoices: Boolean,
        populateIncomeConvertedAmounts: Boolean,
    ): InvoiceMatchingResult {
        val workspace = Tables.WORKSPACE
        // Serialize matching across integrations without blocking foreign-key checks during imports.
        dsl.select(workspace.id).from(workspace).where(workspace.id.eq(workspaceId)).forNoKeyUpdate().fetchOne()
        val matching = linkInvoices(workspaceId)
        val currentWorkspace = workspaces.getWorkspace(workspaceId)
        val owner = users.getUserByUserId(currentWorkspace.ownerId)
        runAs(owner.toSecurityPrincipal()) {
            if (createIncomeFromInvoices) createIncomes(
                workspaceId = workspaceId, integrationId = integrationId, language = owner.i18nSettings.language,
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
        unlinked.forEach { record ->
            val description = record.additionalData ?: return@forEach
            val candidates = dsl.select(invoice.id, invoice.invoiceNumber).from(invoice)
                .join(customer).on(customer.id.eq(invoice.customerId))
                .where(customer.workspaceId.eq(workspaceId))
                .and(invoice.currency.eq(record.currency))
                .and(invoice.amount.eq(record.amountInCents))
                .andNotExists(DSL.selectOne().from(statement).where(statement.linkedInvoiceId.eq(invoice.id)))
                .fetch()
                .filter { candidate ->
                    val number = candidate[invoice.invoiceNumber]!!
                    number.isNotBlank() && description.contains(other = number, ignoreCase = true)
                }
            if (candidates.size == 1) {
                val invoiceId = candidates.single()[invoice.id]!!
                records.save(record.copy(linkedInvoiceId = invoiceId))
                links.add(StatementInvoiceLink(statementId = record.id!!, invoiceId = invoiceId))
            } else if (candidates.size > 1) {
                ambiguous++
            }
        }
        return InvoiceMatchingResult(checked = unlinked.size, links = links, ambiguous = ambiguous)
    }

    private fun createIncomes(workspaceId: String, integrationId: String, language: String) {
        val incomeTable = Tables.INCOME
        unlinkedRecords(workspaceId = workspaceId, integrationId = integrationId, type = StatementType.CREDIT).forEach { record ->
            val invoiceId = record.linkedInvoiceId ?: return@forEach
            val invoice = invoices.getInvoiceByIdAndWorkspaceId(id = invoiceId, workspaceId = workspaceId) ?: return@forEach
            if (invoice.datePaid != null || invoice.status in setOf(InvoiceStatus.PAID, InvoiceStatus.CANCELLED)) return@forEach
            if (dsl.fetchExists(dsl.selectOne().from(incomeTable).where(incomeTable.linkedInvoiceId.eq(invoiceId)))) return@forEach
            val income = incomesService.saveIncome(Income(
                workspaceId = workspaceId,
                categoryId = null,
                title = if (language == "uk") "Платіж за рахунком №${invoice.invoiceNumber}" else "Payment for Invoice #${invoice.invoiceNumber}",
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
        val income = Tables.INCOME
        val credit = Tables.ACCOUNT_STATEMENT_RECORD
        unlinkedRecords(workspaceId = workspaceId, integrationId = integrationId, type = StatementType.CONVERSION).forEach { record ->
            val sourceAmount = record.sourceAmountInCents ?: return@forEach
            val targetAmount = record.targetAmountInCents ?: return@forEach
            if (sourceAmount <= 0 || targetAmount < 0 || record.targetCurrency != defaultCurrency) return@forEach
            val date = record.occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
            val candidates = dsl.selectDistinct(income.id).from(income)
                .join(credit).on(credit.linkedIncomeId.eq(income.id))
                .where(income.workspaceId.eq(workspaceId))
                .and(credit.workspaceId.eq(workspaceId))
                .and(credit.integrationId.eq(integrationId))
                .and(credit.type.eq(StatementType.CREDIT.name))
                .and(income.currency.eq(record.sourceCurrency))
                .and(income.originalAmount.eq(sourceAmount))
                .and(income.convertedOriginalAmountInDefaultCurrency.isNull)
                .and(income.dateReceived.le(date))
                .fetch(income.id)
            if (candidates.size != 1) return@forEach
            val matched = incomes.findByIdAndWorkspaceId(incomeId = candidates.single()!!, workspaceId = workspaceId) ?: return@forEach
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
        }
    }

    private fun unlinkedRecords(workspaceId: String, integrationId: String, type: StatementType) =
        records.findByWorkspaceIdAndIntegrationIdAndLinkedIncomeIdIsNullAndType(
            workspaceId = workspaceId, integrationId = integrationId, type = type,
        ).sortedWith(compareBy<AccountStatementRecord> { it.occurredAt }.thenBy { it.id })

    data class StatementInvoiceLink(val statementId: String, val invoiceId: String)

    data class InvoiceMatchingResult(val checked: Int = 0, val links: List<StatementInvoiceLink> = emptyList(), val ambiguous: Int = 0)
}
