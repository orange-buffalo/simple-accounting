package io.orangebuffalo.simpleaccounting.business.api.dataloaders

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import io.orangebuffalo.simpleaccounting.business.api.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.newAsyncMappedDataLoader
import org.dataloader.DataLoader
import org.springframework.stereotype.Component

private const val NAME = "categoryById"

@Component
class CategoryByIdDataLoader(
    private val categoriesRepository: CategoriesRepository,
) : KotlinDataLoader<Long, CategoryGqlDto?> {

    override val dataLoaderName: String = NAME

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Long, CategoryGqlDto?> =
        newAsyncMappedDataLoader { categoryIds ->
            val categories = categoriesRepository.findAllById(categoryIds)
            categories.associate { it.id!! to CategoryGqlDto(name = it.name) }
        }
}
