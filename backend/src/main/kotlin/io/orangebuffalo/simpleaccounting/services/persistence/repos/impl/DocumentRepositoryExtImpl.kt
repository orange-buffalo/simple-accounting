package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.DocumentRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class DocumentRepositoryExtImpl(
    private val dslContext: DSLContext
) : DocumentRepositoryExt {

    private val document = Tables.DOCUMENT

    override fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Document? = dslContext
        .select()
        .from(document)
        .where(
            document.id.eq(id),
            document.workspaceId.eq(workspaceId)
        )
        .fetchOneOrNull()

    override fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long> = dslContext
        .select(document.id)
        .from(document)
        .where(
            document.id.`in`(ids),
            document.workspaceId.eq(workspaceId)
        )
        .fetchInto(Long::class.java)

}
