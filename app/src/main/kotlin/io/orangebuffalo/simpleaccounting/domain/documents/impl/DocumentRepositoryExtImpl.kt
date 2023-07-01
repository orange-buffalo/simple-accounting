package io.orangebuffalo.simpleaccounting.domain.documents.impl

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class DocumentRepositoryExtImpl(
    private val dslContext: DSLContext
) : DocumentRepositoryExt {

    private val document = Tables.DOCUMENT

    override fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long> = dslContext
        .select(document.id)
        .from(document)
        .where(
            document.id.`in`(ids),
            document.workspaceId.eq(workspaceId)
        )
        .fetchInto(Long::class.java)

}
