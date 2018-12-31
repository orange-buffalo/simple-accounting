package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.accounting.simpleaccounting.services.storage.DocumentStorage
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class DocumentService(
    private val documentStorages: List<DocumentStorage>,
    private val documentRepository: DocumentRepository,
    private val timeService: TimeService
) {

    suspend fun uploadDocument(filePart: FilePart, notes: String?, workspace: Workspace): Document {
        val documentStorage = getDocumentStorageByUser(workspace.owner)
        val storageProviderResponse = documentStorage.saveDocument(filePart, workspace)
        return withDbContext {
            documentRepository.save(
                Document(
                    name = filePart.filename(),
                    notes = notes,
                    timeUploaded = timeService.currentTime(),
                    workspace = workspace,
                    storageProviderId = documentStorage.getId(),
                    storageProviderLocation = storageProviderResponse.storageProviderLocation,
                    sizeInBytes = storageProviderResponse.sizeInBytes
                )
            )
        }
    }

    fun getDocumentStorageByUser(user: PlatformUser) = documentStorages
        .first { it.getId() == "local-fs" }

    suspend fun getDocumentsByIds(ids: List<Long>): List<Document> =
        withDbContext {
            documentRepository.findAllById(ids)
        }

    suspend fun getDocuments(page: Pageable, predicate: Predicate): Page<Document> =
        withDbContext {
            documentRepository.findAll(predicate, page)
        }

    suspend fun getDocumentById(documentId: Long): Document? =
        withDbContext {
            documentRepository.findById(documentId).orElseGet(null)
        }

    suspend fun getDocumentContent(document: Document): Resource {
        return getDocumentStorageById(document.storageProviderId).getDocumentContent(
            document.workspace,
            document.storageProviderLocation ?: throw IllegalStateException("$document has not location assigned")
        )
    }

    private fun getDocumentStorageById(providerId: String) = documentStorages
        .first { it.getId() == providerId }
}