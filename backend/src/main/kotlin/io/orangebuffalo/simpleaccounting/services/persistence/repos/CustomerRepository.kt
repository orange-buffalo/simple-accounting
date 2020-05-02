package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Customer

interface CustomerRepository : AbstractEntityRepository<Customer>, CustomerRepositoryExt

interface CustomerRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer?
    fun existsByIdAndWorkspaceId(customerId: Long, workspaceId: Long): Boolean
}
