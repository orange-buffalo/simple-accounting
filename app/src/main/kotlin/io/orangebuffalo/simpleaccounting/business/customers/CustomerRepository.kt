package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface CustomerRepository : AbstractEntityRepository<Customer> {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}

