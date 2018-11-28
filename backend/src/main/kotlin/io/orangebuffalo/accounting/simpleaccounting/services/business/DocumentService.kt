package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
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
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class DocumentService(
    private val documentStorages: List<DocumentStorage>,
    private val documentRepository: DocumentRepository,
    private val timeService: TimeService
) {

    @Deprecated("migrate to coroutines")
    fun uploadDocument(filePart: FilePart, notes: String?, workspace: Workspace): Mono<Document> {
        val documentStorage = getDocumentStorageByUser(workspace.owner)
        return documentStorage
            .saveDocument(filePart, workspace)
            .map { response ->
                documentRepository.save(
                    Document(
                        name = filePart.filename(),
                        notes = notes,
                        timeUploaded = timeService.currentTime(),
                        workspace = workspace,
                        storageProviderId = documentStorage.getId(),
                        storageProviderLocation = response.storageProviderLocation,
                        sizeInBytes = response.sizeInBytes
                    )
                )
            }
            .subscribeOn(Schedulers.elastic())
    }

    fun getDocumentStorageByUser(user: PlatformUser) = documentStorages
        .first { it.getId() == "local-fs" }

    suspend fun getDocumentsByIds2(ids: List<Long>): List<Document> = withDbContext {
        documentRepository.findAllById(ids)
    }

    @Deprecated("migrate to coroutines")
    fun getDocuments(page: Pageable, predicate: Predicate): Mono<Page<Document>> {
        return Mono.fromSupplier { documentRepository.findAll(predicate, page) }
            .subscribeOn(Schedulers.elastic())
    }

    @Deprecated("migrate to coroutines")
    fun getDocumentById(documentId: Long): Mono<Document> = Mono
        .fromSupplier { documentRepository.findById(documentId) }
        .subscribeOn(Schedulers.elastic())
        .filter { it.isPresent }
        .map { it.get() }

    @Deprecated("migrate to coroutines")
    fun getDocumentContent(document: Document): Mono<Resource> {
        return getDocumentStorageById(document.storageProviderId).getDocumentContent(
            document.workspace,
            document.storageProviderLocation ?: throw IllegalStateException("$document has not location assigned")
        )
    }

    private fun getDocumentStorageById(providerId: String) = documentStorages
        .first { it.getId() == providerId }
}