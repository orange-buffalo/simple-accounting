package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceInvoiceKey(val workspaceId: Long, val invoiceId: Long)

private const val NAME = "invoiceByWorkspaceAndId"

@Component
class InvoiceByWorkspaceAndIdDataLoader(
    private val invoicesRepository: InvoicesRepository,
) : KotlinDataLoader<WorkspaceInvoiceKey, InvoiceGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceInvoiceKey, InvoiceGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val invoiceIds = keys.map { it.invoiceId }.toSet()
            val invoices = invoicesRepository.findAllById(invoiceIds)
            val invoiceMap = invoices.associateBy { it.id!! }
            keys.associateWith { key ->
                invoiceMap[key.invoiceId]?.toInvoiceGqlDto(key.workspaceId)
            }
        }
}

fun DataFetchingEnvironment.loadInvoiceByWorkspaceAndId(
    workspaceId: Long,
    invoiceId: Long,
): CompletableFuture<InvoiceGqlDto?> =
    getDataLoader<WorkspaceInvoiceKey, InvoiceGqlDto?>(NAME)!!.load(WorkspaceInvoiceKey(workspaceId, invoiceId))
