package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeAttachment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
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
    private val processingRepository: AccountStatementProcessingRepository,
    private val incomesService: IncomesService,
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
        val locale = Locale.forLanguageTag(owner.i18nSettings.language)
        if (createIncomeFromInvoices) {
            createIncomes(
                workspaceId = workspaceId,
                integrationId = integrationId,
                locale = locale,
                categoryId = requireNotNull(incomeCategoryId) { "An income category is required for automatic income creation" },
            )
        }
        if (populateIncomeConvertedAmounts) {
            convertIncomes(
                workspaceId = workspaceId,
                integrationId = integrationId,
                defaultCurrency = currentWorkspace.defaultCurrency,
                locale = locale,
            )
        }
        return matching
    }

    private fun linkInvoices(workspaceId: String): InvoiceMatchingResult {
        val unlinked = processingRepository.findCreditsWithoutInvoice(workspaceId)
        val links = mutableListOf<StatementInvoiceLink>()
        var ambiguous = 0
        val availableInvoices = processingRepository.findUnlinkedInvoices(workspaceId).associateBy { it.id }.toMutableMap()
        unlinked.forEach { record ->
            val description = record.additionalData ?: return@forEach
            val candidates = availableInvoices.values.filter { candidate ->
                val number = candidate.invoiceNumber
                candidate.currency == record.currency && candidate.amount == record.amountInCents &&
                    number.isNotBlank() && description.contains(other = number, ignoreCase = true)
            }
            if (candidates.size == 1) {
                val invoiceId = candidates.single().id!!
                records.save(record.copy(linkedInvoiceId = invoiceId))
                availableInvoices.remove(invoiceId)
                links.add(StatementInvoiceLink(statementId = record.id!!, invoiceId = invoiceId))
            } else if (candidates.size > 1) {
                ambiguous++
            }
        }
        return InvoiceMatchingResult(checked = unlinked.size, links = links, ambiguous = ambiguous)
    }

    private fun createIncomes(workspaceId: String, integrationId: String, locale: Locale, categoryId: String) {
        val eligible = processingRepository.findCreditsForIncomeCreation(workspaceId = workspaceId, integrationId = integrationId)
        if (eligible.isEmpty()) return
        val invoicesById = processingRepository.findInvoices(
            ids = eligible.map { it.linkedInvoiceId!! }, workspaceId = workspaceId,
        ).associateBy { it.id }
        eligible.forEach { record ->
            val invoiceId = record.linkedInvoiceId!!
            val invoice = invoicesById[invoiceId] ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
            val income = incomesService.saveIncome(Income(
                workspaceId = workspaceId,
                categoryId = categoryId,
                title = messages.getMessage("income.invoicePayment.title", arrayOf(invoice.invoiceNumber), locale),
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

    private fun convertIncomes(workspaceId: String, integrationId: String, defaultCurrency: String, locale: Locale) {
        val eligibleIncomes = processingRepository.findIncomesForConversion(workspaceId = workspaceId, integrationId = integrationId)
            .associateBy { it.id }.toMutableMap()
        processingRepository.findConversions(workspaceId = workspaceId, integrationId = integrationId, defaultCurrency = defaultCurrency).forEach { record ->
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
            val note = messages.getMessage(
                "income.conversion.note", arrayOf(date.toString(), matched.currency, rate.stripTrailingZeros().toPlainString(), defaultCurrency), locale,
            )
            val updatedNotes = listOfNotNull(matched.notes?.takeIf { it.isNotEmpty() }, note).joinToString("\n")
            incomesService.saveIncome(matched.copy(
                convertedAmounts = AmountsInDefaultCurrency(targetAmount),
                attachments = matched.attachments + record.attachments.map { IncomeAttachment(it.documentId) },
                notes = updatedNotes.takeIf { it.length <= 1024 } ?: matched.notes,
            ))
            records.save(record.copy(linkedIncomeId = matched.id))
            eligibleIncomes.remove(matched.id)
        }
    }

    data class StatementInvoiceLink(val statementId: String, val invoiceId: String)

    data class InvoiceMatchingResult(val checked: Int = 0, val links: List<StatementInvoiceLink> = emptyList(), val ambiguous: Int = 0)
}
