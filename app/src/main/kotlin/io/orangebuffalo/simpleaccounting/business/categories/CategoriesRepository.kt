package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface CategoriesRepository : AbstractEntityRepository<Category> {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Category?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
    fun findAllByWorkspaceIdIn(workspaceIds: Set<Long>): List<Category>
}
