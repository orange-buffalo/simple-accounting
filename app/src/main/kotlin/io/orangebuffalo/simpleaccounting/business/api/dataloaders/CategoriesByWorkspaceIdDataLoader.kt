package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.api.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component

private const val NAME = "categoriesByWorkspaceId"

@Component
class CategoriesByWorkspaceIdDataLoader(
    private val categoriesRepository: CategoriesRepository,
) : KotlinDataLoader<Long, List<CategoryGqlDto>> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, List<CategoryGqlDto>> =
        newAsyncMappedDataLoader { workspaceIds ->
            val categories = categoriesRepository.findAllByWorkspaceIdIn(workspaceIds)
            val grouped = categories.groupBy { it.workspaceId }
            workspaceIds.associateWith { wsId ->
                grouped[wsId]?.map { CategoryGqlDto(name = it.name) } ?: emptyList()
            }
        }
}
