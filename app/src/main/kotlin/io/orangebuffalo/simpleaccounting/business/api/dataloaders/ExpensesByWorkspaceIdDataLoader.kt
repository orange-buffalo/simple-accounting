package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.api.ExpenseGqlDto
import io.orangebuffalo.simpleaccounting.business.expenses.ExpensesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component

private const val NAME = "expensesByWorkspaceId"

@Component
class ExpensesByWorkspaceIdDataLoader(
    private val expensesRepository: ExpensesRepository,
    private val categoryByIdDataLoader: CategoryByIdDataLoader,
) : KotlinDataLoader<Long, List<ExpenseGqlDto>> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, List<ExpenseGqlDto>> =
        newAsyncMappedDataLoader { workspaceIds ->
            val expenses = expensesRepository.findAllByWorkspaceIdIn(workspaceIds)
            val grouped = expenses.groupBy { it.workspaceId }
            workspaceIds.associateWith { wsId ->
                grouped[wsId]?.map {
                    ExpenseGqlDto(
                        title = it.title,
                        categoryId = it.categoryId,
                        categoryByIdDataLoader = categoryByIdDataLoader,
                    )
                } ?: emptyList()
            }
        }
}
