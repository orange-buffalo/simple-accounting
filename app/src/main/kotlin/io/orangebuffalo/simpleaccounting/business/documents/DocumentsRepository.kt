package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface DocumentsRepository : AbstractEntityRepository<Document>, DocumentsRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Document?
}

interface DocumentsRepositoryExt {
    fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long>
    fun getStorageStatsByOwner(ownerId: Long): List<DocumentStorageStatisticsRecord>

    fun findUsagesByDocumentIds(documentIds: Collection<Long>): Map<Long, List<DocumentUsageGqlDto>>
}

data class DocumentStorageStatisticsRecord(
    val storageId: String,
    val documentsCount: Int,
)
