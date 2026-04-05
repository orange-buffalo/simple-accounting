package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("Customer")
@GraphQLDescription("A customer in a workspace.")
data class CustomerGqlDto(
    @GraphQLDescription("ID of the customer.")
    val id: Long,

    @GraphQLDescription("Name of the customer.")
    val name: String,
)
