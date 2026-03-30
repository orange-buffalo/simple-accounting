package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Query
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.customers.CustomersService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.decodeCursor
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CustomersQuery(
    private val customersService: CustomersService,
    private val workspacesService: WorkspacesService,
) : Query {
    @Suppress("unused")
    @GraphQLDescription("Returns all customers in a workspace with cursor-based pagination.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun customers(
        @GraphQLDescription("ID of the workspace.")
        workspaceId: Int,
        @GraphQLDescription("The maximum number of items to return.")
        @Min(GraphqlPaginationConstants.PAGE_SIZE_MIN)
        @Max(GraphqlPaginationConstants.PAGE_SIZE_MAX)
        first: Int,
        @GraphQLDescription("Cursor after which to return items.") after: String? = null,
    ): ConnectionGqlDto<CustomerGqlDto> {
        workspacesService.getAccessibleWorkspace(workspaceId.toLong(), WorkspaceAccessMode.READ_ONLY)
        val cursorPage = decodeCursor(after)
        return customersService.getCustomersPaginated(
            workspaceId = workspaceId.toLong(),
            first = first,
            cursorPage = cursorPage,
        )
    }
}

@GraphQLName("Customer")
@GraphQLDescription("A customer in a workspace.")
data class CustomerGqlDto(
    @GraphQLDescription("ID of the customer.")
    val id: Int,

    @GraphQLDescription("Name of the customer.")
    val name: String,
)
