package io.orangebuffalo.simpleaccounting.business.documents.impl

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.documents.DocumentStorageStatisticsRecord
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepositoryExt
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.springframework.stereotype.Repository

@Repository
class DocumentsRepositoryExtImpl(
    private val dslContext: DSLContext
) : DocumentsRepositoryExt {

    private val document = Tables.DOCUMENT
    private val workspace = Tables.WORKSPACE

    override fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long> = dslContext
        .select(document.id)
        .from(document)
        .where(
            document.id.`in`(ids),
            document.workspaceId.eq(workspaceId)
        )
        .fetchInto(Long::class.java)

    override fun getStorageStatsByOwner(ownerId: Long): List<DocumentStorageStatisticsRecord> = dslContext
        .select(document.storageId, count(document.id))
        .from(document)
        .join(workspace).on(workspace.id.eq(document.workspaceId))
        .where(workspace.ownerId.eq(ownerId))
        .groupBy(document.storageId)
        .fetch()
        .map { record ->
            DocumentStorageStatisticsRecord(
                storageId = record.get(document.storageId)!!,
                documentsCount = record.get(count(document.id)),
            )
        }
}
