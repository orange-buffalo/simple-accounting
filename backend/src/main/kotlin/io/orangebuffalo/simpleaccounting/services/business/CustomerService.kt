package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Customer
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CustomerRepository
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
}
