package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface CustomersRepository : AbstractEntityRepository<Customer> {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}

