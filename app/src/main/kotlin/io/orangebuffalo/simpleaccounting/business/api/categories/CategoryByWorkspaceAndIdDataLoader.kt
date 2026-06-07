package io.orangebuffalo.simpleaccounting.business.api.categories

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

data class WorkspaceCategoryKey(val workspaceId: String, val categoryId: String)

private const val NAME = "categoryByWorkspaceAndId"

@Component
class CategoryByWorkspaceAndIdDataLoader(
    private val categoriesRepository: CategoriesRepository,
) : KotlinDataLoader<WorkspaceCategoryKey, CategoryGqlDto> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<WorkspaceCategoryKey, CategoryGqlDto> =
        newAsyncMappedDataLoader { keys ->
            val categoryIds = keys.map { it.categoryId }.toSet()
            val categories = categoriesRepository.findAllById(categoryIds)
            categories.associate { category ->
                WorkspaceCategoryKey(category.workspaceId, category.id!!) to CategoryGqlDto(
                    id = category.id!!,
                    version = category.version!!,
                    name = category.name,
                    description = category.description,
                    income = category.income,
                    expense = category.expense,
                )
            }
        }
}

fun DataFetchingEnvironment.loadCategoryByWorkspaceAndId(
    workspaceId: String,
    categoryId: String,
): CompletableFuture<CategoryGqlDto?> =
    getDataLoader<WorkspaceCategoryKey, CategoryGqlDto>(NAME)!!.load(WorkspaceCategoryKey(workspaceId, categoryId)).thenApply { it }
