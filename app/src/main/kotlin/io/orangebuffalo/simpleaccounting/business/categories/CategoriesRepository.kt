package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface CategoriesRepository : AbstractEntityRepository<Category> {
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
