package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.api.CustomerGqlDto
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.CursorPage
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.EdgeGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.buildConnection
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.infra.toNullable
import org.springframework.stereotype.Service

@Service
class CustomersService(
    private val customersRepository: CustomersRepository,
    private val workspacesService: WorkspacesService
) {

    suspend fun saveCustomer(customer: Customer): Customer {
        workspacesService.validateWorkspaceAccess(customer.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return withDbContext { customersRepository.save(customer) }
    }

    suspend fun getCustomerByIdAndWorkspace(id: Long, workspaceId: Long): Customer? = withDbContext {
        customersRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun validateCustomer(customerId: Long, workspaceId: Long) = withDbContext {
        if (!customersRepository.existsByIdAndWorkspaceId(customerId, workspaceId)) {
            throw EntityNotFoundException("Customer $customerId is not found")
        }
    }

    suspend fun findById(customerId: Long): Customer? = withDbContext {
        customersRepository.findById(customerId).toNullable()
    }

    suspend fun getCustomersPaginated(
        workspaceId: Long,
        first: Int,
        cursorPage: CursorPage,
    ): ConnectionGqlDto<CustomerGqlDto> = withDbContext {
        val items = customersRepository.findByWorkspaceIdPaginated(
            workspaceId = workspaceId,
            limit = first,
            afterCreatedAt = cursorPage.createdAtAfter,
        )
        val totalCount = customersRepository.countByWorkspaceId(workspaceId)
        buildConnection(
            items = items,
            requestedPageSize = first,
            totalCount = totalCount,
            cursorPage = cursorPage,
            mapper = { customer ->
                EdgeGqlDto(
                    cursor = encodeCursor(customer.createdAt!!),
                    node = CustomerGqlDto(
                        id = customer.id!!.toInt(),
                        name = customer.name,
                    ),
                )
            },
        )
    }
}
