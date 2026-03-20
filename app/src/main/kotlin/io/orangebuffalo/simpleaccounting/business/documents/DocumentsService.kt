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
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
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
            ?: throw IllegalStateException("User ${request.workspace.ownerId} has no documents storage")
        val response = documentStorage.saveDocument(request)
        return withDbContext {
            documentRepository.save(
                Document(
                    name = request.fileName,
                    timeUploaded = timeService.currentTime(),
                    workspaceId = request.workspace.id!!,
                    storageId = documentStorage.getId(),
                    storageLocation = response.storageLocation,
                    sizeInBytes = response.sizeInBytes,
                    mimeType = request.contentType ?: DEFAULT_MIME_TYPE
                )
            )
        }
    }

    private suspend fun getDocumentStorageByUser(userId: Long): DocumentsStorage? {
        val user = platformUsersService.getUserByUserId(userId)
        return documentsStorages.firstOrNull { it.getId() == user.documentsStorage }
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
        return userStorage?.getCurrentUserStorageStatus() ?: DocumentsStorageStatus(false)
    }

    suspend fun getDocumentsStorageStatistics(): List<DocumentStorageStatisticsRecord> {
        val currentUser = platformUsersService.getCurrentUser()
        return withDbContext {
            documentRepository.getStorageStatsByOwner(currentUser.id!!)
        }
    }

    suspend fun getDownloadAvailableStorages(): List<String> = runAsWorkspaceOwnerIfTransient {
        val ownerId = platformUsersService.getCurrentUser().id!!
        documentsStorages
            .filter { it.isDownloadAvailableForUser(ownerId) }
            .map { it.getId() }
            .sorted()
    }

    suspend fun getDownloadToken(workspaceId: Long, documentId: Long): String {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        getDocumentByIdAndWorkspaceId(documentId, workspaceId)
            ?: throw EntityNotFoundException("Document $documentId is not found")
        return runAsWorkspaceOwnerIfTransient {
            downloadsService.createDownloadToken(this@DocumentsService, DocumentDownloadMetadata(documentId))
        }
    }

    private suspend fun <T> runAsWorkspaceOwnerIfTransient(block: suspend () -> T): T {
        val principal = getCurrentPrincipal()
        return if (principal.isTransient) {
            val workspace = workspacesService.getWorkspaceByValidAccessToken(principal.userName)
            val owner = platformUsersService.getUserByUserId(workspace.ownerId)
            runAs(owner.toSecurityPrincipal()) { block() }
        } else {
            block()
        }
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
            contentType = document.mimeType
        )
    }
}

data class DocumentDownloadMetadata(
    val documentId: Long
)
