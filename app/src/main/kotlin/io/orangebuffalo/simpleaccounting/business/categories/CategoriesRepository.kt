package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface CategoriesRepository : AbstractEntityRepository<Category> {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): Category?
    fun existsByIdAndWorkspaceId(id: String, workspaceId: String): Boolean
    fun findAllByWorkspaceIdIn(workspaceIds: Set<String>): List<Category>
}
