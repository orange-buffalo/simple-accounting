package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Customer

interface CustomerRepository : AbstractEntityRepository<Customer> {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}

