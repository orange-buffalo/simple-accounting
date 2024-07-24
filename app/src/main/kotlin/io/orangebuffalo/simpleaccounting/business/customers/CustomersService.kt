package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
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
}
