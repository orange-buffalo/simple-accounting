package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceCategoryKey(val workspaceId: Long, val categoryId: Long)

private const val NAME = "categoryByWorkspaceAndId"

@Component
class CategoryByWorkspaceAndIdDataLoader(
    private val categoriesRepository: CategoriesRepository,
) : KotlinDataLoader<WorkspaceCategoryKey, CategoryGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceCategoryKey, CategoryGqlDto?> =
        newAsyncMappedDataLoader { keys ->
            val categoryIds = keys.map { it.categoryId }.toSet()
            val categories = categoriesRepository.findAllById(categoryIds)
            val categoryMap = categories.associateBy { WorkspaceCategoryKey(it.workspaceId, it.id!!) }
            keys.associateWith { key ->
                categoryMap[key]?.let { category ->
                    CategoryGqlDto(
                        id = category.id!!,
                        name = category.name,
                        description = category.description,
                        income = category.income,
                        expense = category.expense,
                    )
                }
            }
        }
}

fun DataFetchingEnvironment.loadCategoryByWorkspaceAndId(
    workspaceId: Long,
    categoryId: Long,
): CompletableFuture<CategoryGqlDto?> =
    getDataLoader<WorkspaceCategoryKey, CategoryGqlDto?>(NAME)!!.load(WorkspaceCategoryKey(workspaceId, categoryId))
