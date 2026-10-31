package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountStatementInvoiceLinkingService(
    private val records: AccountStatementRecordsRepository,
    private val dsl: DSLContext,
) {
    @Transactional
    fun linkInvoices(workspaceId: String): InvoiceMatchingResult {
        val workspace = Tables.WORKSPACE
        // Serialize matching across integrations without blocking foreign-key checks during imports.
        dsl.select(workspace.id).from(workspace).where(workspace.id.eq(workspaceId)).forNoKeyUpdate().fetchOne()
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

    data class StatementInvoiceLink(val statementId: String, val invoiceId: String)

    data class InvoiceMatchingResult(val checked: Int = 0, val links: List<StatementInvoiceLink> = emptyList(), val ambiguous: Int = 0)
}
