package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.dataloader.DataLoader
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceInvoiceKey(val workspaceId: Long, val invoiceId: Long)

private const val NAME = "invoiceByWorkspaceAndId"

@Component
class InvoiceByWorkspaceAndIdDataLoader(
    private val dslContext: DSLContext,
) : KotlinDataLoader<WorkspaceInvoiceKey, InvoiceGqlDto?> {

    private val invoice = Tables.INVOICE
    private val customer = Tables.CUSTOMER
    private val invoiceAttachments = Tables.INVOICE_ATTACHMENTS

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceInvoiceKey, InvoiceGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val invoiceIds = keys.map { it.invoiceId }.toSet()
            val workspaceIds = keys.map { it.workspaceId }.toSet()

            val records = dslContext
                .select(invoice.asterisk(), customer.workspaceId)
                .from(invoice)
                .join(customer).on(customer.id.eq(invoice.customerId))
                .where(invoice.id.`in`(invoiceIds))
                .and(customer.workspaceId.`in`(workspaceIds))
                .fetch()

            val attachmentsByInvoiceId = dslContext
                .select(invoiceAttachments.invoiceId, invoiceAttachments.documentId)
                .from(invoiceAttachments)
                .where(invoiceAttachments.invoiceId.`in`(invoiceIds))
                .fetch()
                .groupBy(
                    { it[invoiceAttachments.invoiceId]!! },
                    { it[invoiceAttachments.documentId]!! },
                )

            val dtosByKey = records.associate { record ->
                val workspaceId = record[customer.workspaceId]!!
                val invoiceId = record[invoice.id]!!
                WorkspaceInvoiceKey(workspaceId, invoiceId) to InvoiceGqlDto(
                    id = invoiceId,
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
                    attachmentIds = attachmentsByInvoiceId[invoiceId] ?: emptyList(),
                )
            }

            keys.associateWith { key -> dtosByKey[key] }
        }
}

fun DataFetchingEnvironment.loadInvoiceByWorkspaceAndId(
    workspaceId: Long,
    invoiceId: Long,
): CompletableFuture<InvoiceGqlDto?> =
    getDataLoader<WorkspaceInvoiceKey, InvoiceGqlDto?>(NAME)!!.load(WorkspaceInvoiceKey(workspaceId, invoiceId))
