package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import java.time.Instant

interface CategoriesRepository : AbstractEntityRepository<Category>, CategoriesRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Category?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
    fun findAllByWorkspaceIdIn(workspaceIds: Set<Long>): List<Category>
}

interface CategoriesRepositoryExt {
    fun findByWorkspaceIdPaginated(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<Category>

    fun countByWorkspaceId(workspaceId: Long): Int
}
