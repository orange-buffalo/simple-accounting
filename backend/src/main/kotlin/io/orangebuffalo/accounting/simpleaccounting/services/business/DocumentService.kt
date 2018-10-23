package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.DocumentRepository
import io.orangebuffalo.accounting.simpleaccounting.services.storage.DocumentStorage
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.ZonedDateTime

@Service
class DocumentService(
    private val documentStorages: List<DocumentStorage>,
    private val documentRepository: DocumentRepository
) {

    fun uploadDocument(filePart: FilePart, notes: String?, workspace: Workspace): Mono<Document> {
        val documentStorage = getDocumentStorageByUser(workspace.owner)
        return documentStorage
            .saveDocument(filePart, workspace)
            .map { storageProviderLocation ->
                documentRepository.save(
                    Document(
                        name = filePart.filename(),
                        notes = notes,
                        // TODO handle time zones properly
                        dateUploaded = ZonedDateTime.now(),
                        workspace = workspace,
                        storageProviderId = documentStorage.getId(),
                        storageProviderLocation = storageProviderLocation
                    )
                )
            }
            .subscribeOn(Schedulers.elastic())
    }

    fun getDocumentStorageByUser(user: PlatformUser) = documentStorages
        .first { it.getId() == "local-fs" }

}