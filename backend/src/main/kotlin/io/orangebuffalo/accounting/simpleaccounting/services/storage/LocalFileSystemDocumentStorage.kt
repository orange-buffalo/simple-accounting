package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class LocalFileSystemDocumentStorage : DocumentStorage {

    override fun saveDocument(file: FilePart, workspace: Workspace): Mono<String> {
        return Mono.just("fake")
    }

    override fun getId() = "local-fs"

}