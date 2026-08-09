package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadContentResponse
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadableContentProvider
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.business.integration.TokensRepository
import io.orangebuffalo.simpleaccounting.business.integration.getRequestByToken
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DocumentsService(
    private val documentsStorages: List<DocumentsStorage>,
    private val documentRepository: DocumentsRepository,
    private val timeService: TimeService,
    private val workspacesService: WorkspacesService,
    private val platformUsersService: PlatformUsersService,
    private val downloadsService: DownloadsService,
    private val tokensRepository: TokensRepository,
    private val tokenGenerator: TokenGenerator,
) : DownloadableContentProvider<DocumentDownloadMetadata> {

    fun saveDocument(request: SaveDocumentRequest): Document {
        val documentStorage = getDocumentStorageByUser(request.workspace.ownerId)
            ?: throw IllegalStateException("User ${request.workspace.ownerId} has no documents storage")
        val response = documentStorage.saveDocument(request)
        return documentRepository.save(
                Document(
                    name = request.fileName,
                    timeUploaded = timeService.currentTime(),
                    workspaceId = request.workspace.id!!,
                    storageId = documentStorage.getId(),
                    storageLocation = response.storageLocation,
                    sizeInBytes = response.sizeInBytes,
                    mimeType = request.contentType ?: "application/octet-stream"
                )
            )
    }

    private fun getDocumentStorageByUser(userId: String): DocumentsStorage? {
        val user = platformUsersService.getUserByUserId(userId)
        return documentsStorages.firstOrNull { it.getId() == user.documentsStorage }
    }

    fun getDocumentByIdAndWorkspaceId(
        documentId: String,
        workspaceId: String
    ): Document? = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)

    fun getDocumentContent(document: Document): InputStreamProvider {
        val workspace = workspacesService.getWorkspace(document.workspaceId)
        return getDocumentStorageById(document.storageId).getDocumentContent(
            workspace,
            document.storageLocation ?: throw IllegalStateException("$document has not location assigned")
        )
    }

    private fun getDocumentStorageById(storageId: String) = documentsStorages
        .first { it.getId() == storageId }

    fun validateDocuments(workspaceId: String, documentsIds: Collection<String>) {
        val validDocumentsIds = documentRepository.findValidIds(documentsIds, workspaceId)
        val notValidDocumentsIds = documentsIds.minus(validDocumentsIds)
        if (notValidDocumentsIds.isNotEmpty()) {
            throw EntityNotFoundException("Documents $notValidDocumentsIds are not found")
        }
    }

    fun getCurrentUserStorageStatus(): DocumentsStorageStatus {
        val userStorage = getDocumentStorageByUser(platformUsersService.getCurrentUser().id!!)
        return userStorage?.getCurrentUserStorageStatus() ?: DocumentsStorageStatus(false)
    }

    fun getDocumentsStorageStatistics(): List<DocumentStorageStatisticsRecord> {
        val currentUser = platformUsersService.getCurrentUser()
        return documentRepository.getStorageStatsByOwner(currentUser.id!!)
    }

    fun getDownloadAvailableStorages(): List<String> = runAsWorkspaceOwnerIfTransient {
        val ownerId = platformUsersService.getCurrentUser().id!!
        documentsStorages
            .filter { it.isDownloadAvailableForUser(ownerId) }
            .map { it.getId() }
            .sorted()
    }

    fun getDownloadToken(workspaceId: String, documentId: String): String {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        getDocumentByIdAndWorkspaceId(documentId, workspaceId)
            ?: throw EntityNotFoundException("Document $documentId is not found")
        return runAsWorkspaceOwnerIfTransient {
            downloadsService.createDownloadToken(this@DocumentsService, DocumentDownloadMetadata(documentId))
        }
    }

    fun deleteDocument(workspaceId: String, documentId: String) {
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        val document = getDocumentByIdAndWorkspaceId(documentId, workspaceId)
            ?: throw EntityNotFoundException("Document $documentId is not found")

        val documentUsages = documentRepository.findUsagesByDocumentIds(listOf(documentId))[documentId].orEmpty()
        if (documentUsages.isNotEmpty()) {
            throw DocumentIsUsedException(documentId)
        }

        getDocumentStorageById(document.storageId).deleteDocument(
            workspace,
            document.storageLocation ?: throw IllegalStateException("$document has not location assigned")
        )
        documentRepository.delete(document)
    }

    fun getUploadToken(workspaceId: String): String {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        val userName = getCurrentPrincipal().userName
        return tokenGenerator.generateToken(tokenLength = 30)
            .also { token ->
                tokensRepository.storeToken(
                    token, PersistentUploadRequest(
                        workspaceId = workspaceId,
                        userName = userName,
                    )
                )
            }
    }

    fun saveDocumentByUploadToken(
        token: String,
        fileName: String,
        content: InputStreamProvider,
        contentType: String?,
    ): Document {
        val uploadRequest = tokensRepository.getRequestByToken<PersistentUploadRequest>(token)
        val workspace = workspacesService.getWorkspace(uploadRequest.workspaceId)
        val user = platformUsersService.getUserByUserName(uploadRequest.userName)
            ?: throw IllegalStateException("Cannot find user ${uploadRequest.userName}")
        return runAs(user.toSecurityPrincipal()) {
            saveDocument(
                SaveDocumentRequest(
                    fileName = fileName,
                    content = content,
                    workspace = workspace,
                    contentType = contentType,
                )
            )
        }
    }

    private fun <T> runAsWorkspaceOwnerIfTransient(block: () -> T): T {
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

    override fun getContent(metadata: DocumentDownloadMetadata): DownloadContentResponse {
        val document = documentRepository.findByIdOrNull(metadata.documentId)
            ?: throw EntityNotFoundException("Document ${metadata.documentId} is not found")

        return DownloadContentResponse(
            content = getDocumentContent(document),
            fileName = document.name,
            sizeInBytes = document.sizeInBytes,
            contentType = document.mimeType
        )
    }
}

data class DocumentDownloadMetadata(
    val documentId: String
)

data class PersistentUploadRequest(
    val workspaceId: String,
    val userName: String,
)

class DocumentIsUsedException(documentId: String) : RuntimeException("Document $documentId is used and cannot be deleted")
