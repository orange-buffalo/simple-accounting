package io.orangebuffalo.simpleaccounting.business.api.invoices

import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

@Component
class InvoicesGqlApi(
    private val paginationService: GraphqlPaginationService,
    private val dslContext: DSLContext,
) {
    private val invoice = Tables.INVOICE
    private val customer = Tables.CUSTOMER
    private val invoiceAttachments = Tables.INVOICE_ATTACHMENTS

    suspend fun loadInvoices(
        workspaceId: Long,
        first: Int,
        after: String?,
        freeSearchText: String?,
    ): ConnectionGqlDto<InvoiceGqlDto> {
        return paginationService.forTable(invoice)
            .onQuery { it.join(customer).on(customer.id.eq(invoice.customerId)) }
            .addPredicate(customer.workspaceId.eq(workspaceId))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(
                        DSL.or(
                            invoice.notes.containsIgnoreCase(freeSearchText),
                            invoice.title.containsIgnoreCase(freeSearchText),
                            customer.name.containsIgnoreCase(freeSearchText),
                        )
                    )
                }
            }
            .page(
                first = first,
                after = after,
                sortFields = listOf(invoice.dateIssued.desc(), invoice.createdAt.asc()),
                mapQueryRecord = { record ->
                    InvoiceGqlDto(
                        id = record[invoice.id]!!,
                        version = record[invoice.version]!!,
                        title = record[invoice.title]!!,
                        dateIssued = record[invoice.dateIssued]!!,
                        dateSent = record[invoice.dateSent],
                        datePaid = record[invoice.datePaid],
                        timeCancelled = record[invoice.timeCancelled],
                        dueDate = record[invoice.dueDate]!!,
                        currency = record[invoice.currency]!!,
                        amount = record[invoice.amount]!!,
                        notes = record[invoice.notes],
                        createdAt = record[invoice.createdAt]!!,
                        status = record[invoice.status]!!,
                        generalTaxId = record[invoice.generalTaxId],
                        customerId = record[invoice.customerId]!!,
                        workspaceId = workspaceId,
                        attachmentIds = emptyList(),
                    )
                },
                postProcess = { records ->
                    val attachmentsByInvoiceId = dslContext
                        .select(invoiceAttachments.invoiceId, invoiceAttachments.documentId)
                        .from(invoiceAttachments)
                        .where(invoiceAttachments.invoiceId.`in`(records.map { it.id }))
                        .fetch()
                        .groupBy(
                            { it[invoiceAttachments.invoiceId]!! },
                            { it[invoiceAttachments.documentId]!! },
                        )
                    records.map { dto ->
                        dto.copy(attachmentIds = attachmentsByInvoiceId[dto.id] ?: emptyList())
                    }
                },
            )
    }
}
