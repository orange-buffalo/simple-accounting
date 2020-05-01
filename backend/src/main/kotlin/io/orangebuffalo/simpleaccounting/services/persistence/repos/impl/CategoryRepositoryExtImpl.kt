package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CategoryRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CategoryRepositoryExtImpl(
    private val dslContext: DSLContext
) : CategoryRepositoryExt {
    private val category = Tables.CATEGORY

    override fun existsByWorkspaceIdAndId(id: Long, workspaceId: Long): Boolean = dslContext
        .fetchExists(
            category,
            category.id.eq(id),
            category.workspaceId.eq(workspaceId)
        )
}
