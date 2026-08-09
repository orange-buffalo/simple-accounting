package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.toNullable
import org.springframework.stereotype.Service

@Service
class CustomersService(
    private val customersRepository: CustomersRepository,
    private val workspacesService: WorkspacesService
) {

    fun saveCustomer(customer: Customer): Customer {
        workspacesService.validateWorkspaceAccess(customer.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return customersRepository.save(customer)
    }

    fun getCustomerByIdAndWorkspace(id: String, workspaceId: String): Customer? =
        customersRepository.findByIdAndWorkspaceId(id, workspaceId)

    fun validateCustomer(customerId: String, workspaceId: String) {
        if (!customersRepository.existsByIdAndWorkspaceId(customerId, workspaceId)) {
            throw EntityNotFoundException("Customer $customerId is not found")
        }
    }

    fun findById(customerId: String): Customer? = customersRepository.findById(customerId).toNullable()
}
