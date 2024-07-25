package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface CategoriesRepository : AbstractEntityRepository<Category> {
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
