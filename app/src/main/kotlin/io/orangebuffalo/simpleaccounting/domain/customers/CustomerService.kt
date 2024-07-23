package io.orangebuffalo.simpleaccounting.domain.customers

import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.support.toNullable
import org.springframework.stereotype.Service

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val workspaceService: WorkspaceService
) {

    suspend fun saveCustomer(customer: Customer): Customer {
        workspaceService.validateWorkspaceAccess(customer.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return withDbContext { customerRepository.save(customer) }
    }

    suspend fun getCustomerByIdAndWorkspace(id: Long, workspaceId: Long): Customer? = withDbContext {
        customerRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun validateCustomer(customerId: Long, workspaceId: Long) = withDbContext {
        if (!customerRepository.existsByIdAndWorkspaceId(customerId, workspaceId)) {
            throw EntityNotFoundException("Customer $customerId is not found")
        }
    }

    suspend fun findById(customerId: Long): Customer? = withDbContext {
        customerRepository.findById(customerId).toNullable()
    }
}
