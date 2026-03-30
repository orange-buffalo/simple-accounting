package io.orangebuffalo.simpleaccounting.business.categories.impl

import io.orangebuffalo.simpleaccounting.business.categories.CategoriesRepositoryExt
import io.orangebuffalo.simpleaccounting.business.categories.Category
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class CategoriesRepositoryExtImpl(
    private val dslContext: DSLContext
) : CategoriesRepositoryExt {

    private val category = Tables.CATEGORY

    override fun findByWorkspaceIdPaginated(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<Category> {
        var query = dslContext
            .select(*category.fields())
            .from(category)
            .where(category.workspaceId.eq(workspaceId))

        if (afterCreatedAt != null) {
            query = query.and(category.createdAt.gt(afterCreatedAt))
        }

        return query
            .orderBy(category.createdAt.asc())
            .limit(limit + 1)
            .fetchListOf()
    }

    override fun countByWorkspaceId(workspaceId: Long): Int = dslContext
        .selectCount()
        .from(category)
        .where(category.workspaceId.eq(workspaceId))
        .fetchOne(0, Int::class.java)!!
}
