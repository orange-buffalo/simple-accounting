package io.orangebuffalo.simpleaccounting.business.api.categories

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.categories.Category

@GraphQLName("Category")
@GraphQLDescription("Category of incomes or expenses.")
data class CategoryGqlDto(
    @GraphQLDescription("ID of the category.")
    val id: Long,

    @GraphQLDescription("Name of the category.")
    val name: String,

    @GraphQLDescription("Description of the category.")
    val description: String?,

    @GraphQLDescription("Whether this category is used for incomes.")
    val income: Boolean,

    @GraphQLDescription("Whether this category is used for expenses.")
    val expense: Boolean,
)

fun Category.toCategoryGqlDto() = CategoryGqlDto(
    id = id!!,
    name = name,
    description = description,
    income = income,
    expense = expense,
)
