package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface CustomersRepository : AbstractEntityRepository<Customer> {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): Customer?
    fun existsByIdAndWorkspaceId(id: String, workspaceId: String): Boolean
}

