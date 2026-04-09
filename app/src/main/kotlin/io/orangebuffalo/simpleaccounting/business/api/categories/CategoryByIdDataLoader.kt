package io.orangebuffalo.simpleaccounting.business.api.categories

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
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
            categories.associate {
                it.id!! to CategoryGqlDto(
                    id = it.id!!,
                    name = it.name,
                    description = it.description,
                    income = it.income,
                    expense = it.expense,
                )
            }
        }
}

fun DataFetchingEnvironment.loadCategoryById(
    categoryId: Long,
): CompletableFuture<CategoryGqlDto?> = getDataLoader<Long, CategoryGqlDto?>(NAME)!!.load(categoryId)
