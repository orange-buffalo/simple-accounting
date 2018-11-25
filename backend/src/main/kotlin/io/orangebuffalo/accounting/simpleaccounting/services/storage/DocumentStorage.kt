package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.core.io.Resource
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Mono

interface DocumentStorage {

    fun saveDocument(file: FilePart, workspace: Workspace): Mono<String>

    fun getId(): String

    fun getDocumentContent(workspace: Workspace, storageLocation: String): Mono<Resource>
}