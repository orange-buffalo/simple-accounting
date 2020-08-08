package io.orangebuffalo.simpleaccounting.domain.documents

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadContentResponse
import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadableContentProvider
import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentRequest
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class DocumentsService(
    private val documentsStorages: List<DocumentsStorage>,
    private val documentRepository: DocumentRepository,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService,
    private val platformUserService: PlatformUserService,
    private val downloadsService: DownloadsService
) : DownloadableContentProvider<DocumentDownloadMetadata> {

    suspend fun saveDocument(request: SaveDocumentRequest): Document {
        val documentStorage = getDocumentStorageByUser(request.workspace.ownerId)
        val response = documentStorage.saveDocument(request)
        return withDbContext {
            documentRepository.save(
                Document(
                    name = request.fileName,
                    timeUploaded = timeService.currentTime(),
                    workspaceId = request.workspace.id!!,
                    storageId = documentStorage.getId(),
                    storageLocation = response.storageLocation,
                    sizeInBytes = response.sizeInBytes
                )
            )
        }
    }

    private suspend fun getDocumentStorageByUser(userId: Long): DocumentsStorage {
        val user = platformUserService.getUserByUserId(userId)
        return documentsStorages.first { it.getId() == user.documentsStorage }
    }

    suspend fun getDocumentByIdAndWorkspaceId(
        documentId: Long,
        workspaceId: Long
    ): Document? = withDbContext {
        documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
    }

    suspend fun getDocumentContent(document: Document): Flux<DataBuffer> {
        val workspace = workspaceService.getWorkspace(document.workspaceId)
        return getDocumentStorageById(document.storageId).getDocumentContent(
            workspace,
            document.storageLocation ?: throw IllegalStateException("$document has not location assigned")
        )
    }

    private fun getDocumentStorageById(storageId: String) = documentsStorages
        .first { it.getId() == storageId }

    suspend fun validateDocuments(workspaceId: Long, documentsIds: Collection<Long>) {
        val validDocumentsIds = withDbContext {
            documentRepository.findValidIds(documentsIds, workspaceId)
        }
        val notValidDocumentsIds = documentsIds.minus(validDocumentsIds)
        if (notValidDocumentsIds.isNotEmpty()) {
            throw EntityNotFoundException("Documents $notValidDocumentsIds are not found")
        }
    }

    suspend fun getCurrentUserStorageStatus(): DocumentsStorageStatus {
        val userStorage = getDocumentStorageByUser(platformUserService.getCurrentUser().id!!)
        return userStorage.getCurrentUserStorageStatus()
    }

    suspend fun getDownloadToken(workspaceId: Long, documentId: Long): String {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        getDocumentByIdAndWorkspaceId(documentId, workspaceId)
            ?: throw EntityNotFoundException("Document $documentId is not found")
        return downloadsService.createDownloadToken(this, DocumentDownloadMetadata(documentId))
    }

    override fun getId(): String = DocumentsService::class.simpleName!!

    override suspend fun getContent(metadata: DocumentDownloadMetadata): DownloadContentResponse {
        val document = withDbContext {
            documentRepository.findByIdOrNull(metadata.documentId)
        } ?: throw EntityNotFoundException("Document ${metadata.documentId} is not found")

        return DownloadContentResponse(
            content = getDocumentContent(document),
            fileName = document.name,
            sizeInBytes = document.sizeInBytes,
            // todo #108
            contentType = null
        )
    }
}

data class DocumentDownloadMetadata(
    val documentId: Long
)
