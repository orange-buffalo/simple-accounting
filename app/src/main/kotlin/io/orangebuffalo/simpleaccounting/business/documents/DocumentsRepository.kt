package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface DocumentsRepository : AbstractEntityRepository<Document>, DocumentsRepositoryExt {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): Document?
}

interface DocumentsRepositoryExt {
    fun findValidIds(ids: Collection<String>, workspaceId: String): Collection<String>
    fun getStorageStatsByOwner(ownerId: String): List<DocumentStorageStatisticsRecord>
    fun findIdsByOwnerAndStorageIdNot(ownerId: String, storageId: String): List<String>

    fun findUsagesByDocumentIds(documentIds: Collection<String>): Map<String, List<DocumentUsageGqlDto>>
}

data class DocumentStorageStatisticsRecord(
    val storageId: String,
    val documentsCount: Int,
)
