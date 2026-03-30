package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface CustomersRepository : AbstractEntityRepository<Customer>, CustomersRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}

interface CustomersRepositoryExt

