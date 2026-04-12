package io.orangebuffalo.simpleaccounting.business.api.expenses

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.expenses.ExpensesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceExpenseKey(val workspaceId: Long, val expenseId: Long)

private const val NAME = "expenseByWorkspaceAndId"

@Component
class ExpenseByWorkspaceAndIdDataLoader(
    private val expensesRepository: ExpensesRepository,
) : KotlinDataLoader<WorkspaceExpenseKey, ExpenseGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceExpenseKey, ExpenseGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val expenseIds = keys.map { it.expenseId }.toSet()
            val expenses = expensesRepository.findAllById(expenseIds)
            val expenseMap = expenses.associateBy { WorkspaceExpenseKey(it.workspaceId, it.id!!) }
            keys.associateWith { key -> expenseMap[key]?.toExpenseGqlDto() }
        }
}

fun DataFetchingEnvironment.loadExpenseByWorkspaceAndId(
    workspaceId: Long,
    expenseId: Long,
): CompletableFuture<ExpenseGqlDto?> =
    getDataLoader<WorkspaceExpenseKey, ExpenseGqlDto?>(NAME)!!.load(WorkspaceExpenseKey(workspaceId, expenseId))
