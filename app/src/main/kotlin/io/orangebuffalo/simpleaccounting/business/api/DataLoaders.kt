package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesRepository
import io.orangebuffalo.simpleaccounting.business.expenses.ExpensesRepository
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class CategoriesByWorkspaceIdDataLoader(
    private val categoriesRepository: CategoriesRepository,
) : KotlinDataLoader<Long, List<CategoryGqlDto>> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, List<CategoryGqlDto>> =
        DataLoaderFactory.newMappedDataLoader { workspaceIds ->
            CompletableFuture.supplyAsync {
                val categories = categoriesRepository.findAllByWorkspaceIdIn(workspaceIds)
                val grouped = categories.groupBy { it.workspaceId }
                workspaceIds.associateWith { wsId ->
                    grouped[wsId]?.map { CategoryGqlDto(name = it.name) } ?: emptyList()
                }
            }
        }

    companion object {
        const val NAME = "categoriesByWorkspaceId"
    }
}

@Component
class CategoryByIdDataLoader(
    private val categoriesRepository: CategoriesRepository,
) : KotlinDataLoader<Long, CategoryGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, CategoryGqlDto?> =
        DataLoaderFactory.newMappedDataLoader { categoryIds ->
            CompletableFuture.supplyAsync {
                val categories = categoriesRepository.findAllById(categoryIds)
                categories.associate { it.id!! to CategoryGqlDto(name = it.name) }
            }
        }

    companion object {
        const val NAME = "categoryById"
    }
}

@Component
class ExpensesByWorkspaceIdDataLoader(
    private val expensesRepository: ExpensesRepository,
) : KotlinDataLoader<Long, List<ExpenseGqlDto>> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, List<ExpenseGqlDto>> =
        DataLoaderFactory.newMappedDataLoader { workspaceIds ->
            CompletableFuture.supplyAsync {
                val expenses = expensesRepository.findAllByWorkspaceIdIn(workspaceIds)
                val grouped = expenses.groupBy { it.workspaceId }
                workspaceIds.associateWith { wsId ->
                    grouped[wsId]?.map { ExpenseGqlDto(title = it.title, categoryId = it.categoryId) }
                        ?: emptyList()
                }
            }
        }

    companion object {
        const val NAME = "expensesByWorkspaceId"
    }
}
