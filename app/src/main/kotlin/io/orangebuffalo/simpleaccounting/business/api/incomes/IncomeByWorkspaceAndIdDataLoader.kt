package io.orangebuffalo.simpleaccounting.business.api.incomes

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceIncomeKey(val workspaceId: String, val incomeId: String)

private const val NAME = "incomeByWorkspaceAndId"

@Component
class IncomeByWorkspaceAndIdDataLoader(
    private val incomesRepository: IncomesRepository,
) : KotlinDataLoader<WorkspaceIncomeKey, IncomeGqlDto> {
    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceIncomeKey, IncomeGqlDto> =
        newMappedDataLoader { keys ->
            incomesRepository.findAllById(keys.map { it.incomeId }.toSet())
                .filter { WorkspaceIncomeKey(workspaceId = it.workspaceId, incomeId = it.id!!) in keys }
                .associate {
                    WorkspaceIncomeKey(workspaceId = it.workspaceId, incomeId = it.id!!) to it.toIncomeGqlDto()
                }
        }
}

fun DataFetchingEnvironment.loadIncomeByWorkspaceAndId(
    workspaceId: String,
    incomeId: String,
): CompletableFuture<IncomeGqlDto?> =
    getDataLoader<WorkspaceIncomeKey, IncomeGqlDto>(NAME)!!
        .load(WorkspaceIncomeKey(workspaceId = workspaceId, incomeId = incomeId)).thenApply { it }
