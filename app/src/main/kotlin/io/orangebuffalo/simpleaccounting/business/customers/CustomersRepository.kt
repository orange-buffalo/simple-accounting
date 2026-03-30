package io.orangebuffalo.simpleaccounting.business.customers

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import java.time.Instant

interface CustomersRepository : AbstractEntityRepository<Customer>, CustomersRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}

interface CustomersRepositoryExt {
    fun findByWorkspaceIdPaginated(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<Customer>

    fun countByWorkspaceId(workspaceId: Long): Int
}

