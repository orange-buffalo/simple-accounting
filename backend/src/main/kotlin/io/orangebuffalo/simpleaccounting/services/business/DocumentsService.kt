package io.orangebuffalo.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QDocument
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class DocumentsService(
    private val documentsStorages: List<DocumentsStorage>,
    private val documentRepository: DocumentRepository,
    private val timeService: TimeService
) {

    suspend fun uploadDocument(filePart: FilePart, workspace: Workspace): Document {
        val documentStorage = getDocumentStorageByUser(workspace.owner)
        val storageProviderResponse = documentStorage.saveDocument(filePart, workspace)
        return withDbContext {
            documentRepository.save(
                Document(
                    name = filePart.filename(),
                    timeUploaded = timeService.currentTime(),
                    workspace = workspace,
                    storageProviderId = documentStorage.getId(),
                    storageProviderLocation = storageProviderResponse.storageProviderLocation,
                    sizeInBytes = storageProviderResponse.sizeInBytes
                )
            )
        }
    }

    fun getDocumentStorageByUser(user: PlatformUser) = documentsStorages
        .first { it.getId() == user.documentsStorage }

    suspend fun getDocuments(workspace: Workspace, page: Pageable, predicate: Predicate): Page<Document> =
        withDbContext {
            documentRepository.findAll(QDocument.document.workspace.eq(workspace).and(predicate), page)
        }

    suspend fun getDocumentByIdAndWorkspace(
        documentId: Long,
        workspace: Workspace
    ): Document? =
        withDbContext {
            documentRepository.findByIdAndWorkspace(documentId, workspace)
        }

    suspend fun getDocumentContent(document: Document): Flux<DataBuffer> {
        return getDocumentStorageById(document.storageProviderId).getDocumentContent(
            document.workspace,
            document.storageProviderLocation ?: throw IllegalStateException("$document has not location assigned")
        )
    }

    private fun getDocumentStorageById(providerId: String) = documentsStorages
        .first { it.getId() == providerId }
}
