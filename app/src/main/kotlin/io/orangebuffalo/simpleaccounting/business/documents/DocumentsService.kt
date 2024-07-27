package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadContentResponse
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadableContentProvider
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DocumentsService(
    private val documentsStorages: List<DocumentsStorage>,
    private val documentRepository: DocumentsRepository,
    private val timeService: TimeService,
    private val workspacesService: WorkspacesService,
    private val platformUsersService: PlatformUsersService,
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
        val user = platformUsersService.getUserByUserId(userId)
        return documentsStorages.first { it.getId() == user.documentsStorage }
    }

    suspend fun getDocumentByIdAndWorkspaceId(
        documentId: Long,
        workspaceId: Long
    ): Document? = withDbContext {
        documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
    }

    suspend fun getDocumentContent(document: Document): Flow<DataBuffer> {
        val workspace = workspacesService.getWorkspace(document.workspaceId)
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
        val userStorage = getDocumentStorageByUser(platformUsersService.getCurrentUser().id!!)
        return userStorage.getCurrentUserStorageStatus()
    }

    suspend fun getDownloadToken(workspaceId: Long, documentId: Long): String {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
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
