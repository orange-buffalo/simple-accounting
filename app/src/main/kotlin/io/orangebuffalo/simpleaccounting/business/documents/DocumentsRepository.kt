package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import java.time.Instant

interface DocumentsRepository : AbstractEntityRepository<Document>, DocumentsRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Document?
}

interface DocumentsRepositoryExt {
    fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long>
    fun getStorageStatsByOwner(ownerId: Long): List<DocumentStorageStatisticsRecord>

    fun findByWorkspaceIdPaginated(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<DocumentRecord>

    fun findUsagesByDocumentIds(documentIds: Collection<Long>): Map<Long, List<DocumentUsageGqlDto>>

    fun countByWorkspaceId(workspaceId: Long): Int
}

data class DocumentStorageStatisticsRecord(
    val storageId: String,
    val documentsCount: Int,
)

data class DocumentRecord(
    val id: Long,
    val version: Int,
    val name: String,
    val timeUploaded: Instant,
    val sizeInBytes: Long?,
    val storageId: String,
    val mimeType: String,
    val createdAt: Instant,
)
