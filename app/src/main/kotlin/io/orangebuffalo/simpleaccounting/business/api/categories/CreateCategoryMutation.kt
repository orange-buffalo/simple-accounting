package io.orangebuffalo.simpleaccounting.business.api.categories

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.categories.Category
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateCategoryMutation(
    private val categoriesService: CategoriesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new category in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createCategory(
        @GraphQLDescription("ID of the workspace to create the category in.")
        workspaceId: Long,
        @GraphQLDescription("Name of the category.")
        @NotBlank
        @Size(max = 255)
        name: String,
        @GraphQLDescription("Description of the category.")
        @Size(max = 1000)
        description: String? = null,
        @GraphQLDescription("Whether this category is used for incomes.")
        income: Boolean,
        @GraphQLDescription("Whether this category is used for expenses.")
        expense: Boolean,
    ): CategoryGqlDto {
        val category = categoriesService.createCategory(
            Category(
                name = name,
                workspaceId = workspaceId,
                expense = expense,
                income = income,
                description = description,
            )
        )
        return category.toCategoryGqlDto()
    }
}
