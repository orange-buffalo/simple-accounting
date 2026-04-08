package io.orangebuffalo.simpleaccounting.business.api.customers

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.customers.CustomersService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class EditCustomerMutation(
    private val customersService: CustomersService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing customer in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editCustomer(
        @GraphQLDescription("ID of the workspace the customer belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the customer to update.")
        id: Long,
        @GraphQLDescription("New name of the customer.")
        @NotBlank
        @Size(max = 255)
        name: String,
    ): CustomerGqlDto {
        val customer = customersService.getCustomerByIdAndWorkspace(id, workspaceId)
            ?: throw EntityNotFoundException("Customer $id is not found")

        customer.name = name

        return customersService.saveCustomer(customer).toCustomerGqlDto()
    }
}
