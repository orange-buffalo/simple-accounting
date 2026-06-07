package io.orangebuffalo.simpleaccounting.business.api.categories

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.categories.Category

@GraphQLName("Category")
@GraphQLDescription("Category of incomes or expenses.")
data class CategoryGqlDto(
    @GraphQLDescription("ID of the category.")
    val id: String,

    @GraphQLDescription("Version of the category state.")
    val version: Int,

    @GraphQLDescription("Name of the category.")
    val name: String,

    @GraphQLDescription("Description of the category.")
    val description: String?,

    @GraphQLDescription("Whether this category is used for incomes.")
    val income: Boolean,

    @GraphQLDescription("Whether this category is used for expenses.")
    val expense: Boolean,
)

@GraphQLName("CategoryType")
@GraphQLDescription("Category usage type.")
enum class CategoryTypeGqlDto {
    INCOME,
    EXPENSE,
}

fun Category.toCategoryGqlDto() = CategoryGqlDto(
    id = id!!,
    version = version!!,
    name = name,
    description = description,
    income = income,
    expense = expense,
)
