package io.orangebuffalo.simpleaccounting.business.api.incomes

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceInvoiceIncomesKey(val workspaceId: String, val invoiceId: String)

private const val NAME = "incomesByInvoice"

@Component
class IncomesByInvoiceDataLoader(
    private val incomesRepository: IncomesRepository,
) : KotlinDataLoader<WorkspaceInvoiceIncomesKey, List<IncomeGqlDto>> {
    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceInvoiceIncomesKey, List<IncomeGqlDto>> =
        newMappedDataLoader { keys ->
            keys.groupBy { it.workspaceId }.flatMap { (workspaceId, workspaceKeys) ->
                val incomes = incomesRepository.findAllByWorkspaceIdAndLinkedInvoiceIdIn(
                    workspaceId = workspaceId,
                    invoiceIds = workspaceKeys.map { it.invoiceId }.toSet(),
                ).groupBy { it.linkedInvoiceId }
                workspaceKeys.map { key -> key to incomes[key.invoiceId].orEmpty().map { it.toIncomeGqlDto() } }
            }.toMap()
        }
}

fun DataFetchingEnvironment.loadIncomesByInvoice(
    workspaceId: String,
    invoiceId: String,
): CompletableFuture<List<IncomeGqlDto>> =
    getDataLoader<WorkspaceInvoiceIncomesKey, List<IncomeGqlDto>>(NAME)!!
        .load(WorkspaceInvoiceIncomesKey(workspaceId = workspaceId, invoiceId = invoiceId))
