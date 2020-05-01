package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category

interface CategoryRepository : AbstractEntityRepository<Category>, CategoryRepositoryExt

interface CategoryRepositoryExt {
    fun existsByWorkspaceIdAndId(id: Long, workspaceId: Long): Boolean
}
