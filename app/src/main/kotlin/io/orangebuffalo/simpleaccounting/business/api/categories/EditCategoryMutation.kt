package io.orangebuffalo.simpleaccounting.business.api.categories

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class EditCategoryMutation(
    private val categoriesService: CategoriesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing category in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editCategory(
        @GraphQLDescription("ID of the workspace the category belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the category to update.")
        id: Long,
        @GraphQLDescription("New name of the category.")
        @NotBlank
        @Size(max = 255)
        name: String,
        @GraphQLDescription("New description of the category.")
        @Size(max = 1000)
        description: String?,
        @GraphQLDescription("Whether this category is used for incomes.")
        income: Boolean,
        @GraphQLDescription("Whether this category is used for expenses.")
        expense: Boolean,
    ): CategoryGqlDto {
        val category = categoriesService.getCategoryByIdAndWorkspace(id, workspaceId)
            ?: throw EntityNotFoundException("Category $id is not found")

        category.name = name
        category.description = description
        category.income = income
        category.expense = expense

        return categoriesService.saveCategory(category).toCategoryGqlDto()
    }
}
