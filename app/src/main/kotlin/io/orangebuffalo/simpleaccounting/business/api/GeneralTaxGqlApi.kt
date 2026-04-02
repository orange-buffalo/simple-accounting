package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("GeneralTax")
@GraphQLDescription("General tax applicable to incomes or expenses.")
data class GeneralTaxGqlDto(
    @GraphQLDescription("ID of the general tax.")
    val id: Int,

    @GraphQLDescription("Title of the general tax.")
    val title: String,

    @GraphQLDescription("Description of the general tax.")
    val description: String?,

    @GraphQLDescription("Rate of the general tax in basis points (1/100 of a percent).")
    val rateInBps: Int,
)
