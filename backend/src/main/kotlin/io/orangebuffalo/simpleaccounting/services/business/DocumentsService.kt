package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class DocumentsService(
    private val documentsStorages: List<DocumentsStorage>,
    private val documentRepository: DocumentRepository,
    private val timeService: TimeService,
    private val workspaceService: WorkspaceService
) {

    suspend fun uploadDocument(filePart: FilePart, workspace: Workspace): Document {
        val documentStorage = getDocumentStorageByUser(workspace.owner)
        val storageProviderResponse = documentStorage.saveDocument(filePart, workspace)
        return withDbContext {
            documentRepository.save(
                Document(
                    name = filePart.filename(),
                    timeUploaded = timeService.currentTime(),
                    workspaceId = workspace.id!!,
                    storageProviderId = documentStorage.getId(),
                    storageProviderLocation = storageProviderResponse.storageProviderLocation,
                    sizeInBytes = storageProviderResponse.sizeInBytes
                )
            )
        }
    }

    fun getDocumentStorageByUser(user: PlatformUser) = documentsStorages
        .first { it.getId() == user.documentsStorage }

    suspend fun getDocumentByIdAndWorkspaceId(
        documentId: Long,
        workspaceId: Long
    ): Document? = withDbContext {
        documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
    }

    suspend fun getDocumentContent(document: Document): Flux<DataBuffer> {
        val workspace = workspaceService.getWorkspace(document.workspaceId)
        return getDocumentStorageById(document.storageProviderId).getDocumentContent(
            workspace,
            document.storageProviderLocation ?: throw IllegalStateException("$document has not location assigned")
        )
    }

    private fun getDocumentStorageById(providerId: String) = documentsStorages
        .first { it.getId() == providerId }

    suspend fun validateDocuments(workspaceId: Long, documentsIds: Collection<Long>) {
        val validDocumentsIds = withDbContext {
            documentRepository.findValidIds(documentsIds, workspaceId)
        }
        val notValidDocumentsIds = documentsIds.minus(validDocumentsIds)
        if (notValidDocumentsIds.isNotEmpty()) {
            throw EntityNotFoundException("Documents $notValidDocumentsIds are not found")
        }
    }
}
