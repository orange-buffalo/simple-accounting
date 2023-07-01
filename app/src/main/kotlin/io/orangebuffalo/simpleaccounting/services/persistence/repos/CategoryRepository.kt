package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category

interface CategoryRepository : AbstractEntityRepository<Category> {
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
