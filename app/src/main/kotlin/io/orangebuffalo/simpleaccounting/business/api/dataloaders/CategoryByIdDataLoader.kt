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

fun DataFetchingEnvironment.loadCategoryById(
    categoryId: Long,
): CompletableFuture<CategoryGqlDto?> = getDataLoader<Long, CategoryGqlDto?>(NAME)!!.load(categoryId)
