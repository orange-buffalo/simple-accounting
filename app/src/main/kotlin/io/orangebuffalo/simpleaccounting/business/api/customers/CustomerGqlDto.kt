package io.orangebuffalo.simpleaccounting.business.api.customers

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.orangebuffalo.simpleaccounting.business.customers.Customer

@GraphQLName("Customer")
@GraphQLDescription("A customer in a workspace.")
data class CustomerGqlDto(
    @GraphQLDescription("ID of the customer.")
    val id: Long,

    @GraphQLDescription("Name of the customer.")
    val name: String,
)

fun Customer.toCustomerGqlDto() = CustomerGqlDto(
    id = id!!,
    name = name,
)
