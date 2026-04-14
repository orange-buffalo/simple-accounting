package io.orangebuffalo.simpleaccounting.business.api.incomes

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceIncomeKey(val workspaceId: Long, val incomeId: Long)

private const val NAME = "incomeByWorkspaceAndId"

@Component
class IncomeByWorkspaceAndIdDataLoader(
    private val incomesRepository: IncomesRepository,
) : KotlinDataLoader<WorkspaceIncomeKey, IncomeGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceIncomeKey, IncomeGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val incomeIds = keys.map { it.incomeId }.toSet()
            val incomes = incomesRepository.findAllById(incomeIds)
            val incomeMap = incomes.associateBy { WorkspaceIncomeKey(it.workspaceId, it.id!!) }
            keys.associateWith { key -> incomeMap[key]?.toIncomeGqlDto() }
        }
}

fun DataFetchingEnvironment.loadIncomeByWorkspaceAndId(
    workspaceId: Long,
    incomeId: Long,
): CompletableFuture<IncomeGqlDto?> =
    getDataLoader<WorkspaceIncomeKey, IncomeGqlDto?>(NAME)!!.load(WorkspaceIncomeKey(workspaceId, incomeId))
