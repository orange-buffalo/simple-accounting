package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Customer
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QCustomer
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CustomerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {

    suspend fun saveCustomer(customer: Customer): Customer {
        return withDbContext {
            customerRepository.save(customer)
        }
    }

    suspend fun getCustomers(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Customer> = withDbContext {
        customerRepository.findAll(QCustomer.customer.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getCustomerByIdAndWorkspace(id: Long, workspace: Workspace): Customer? =
        withDbContext {
            customerRepository.findByIdAndWorkspace(id, workspace)
        }
}