package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.core.io.Resource
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Mono

interface DocumentStorage {

    @Deprecated("migrate to coroutines")
    fun saveDocument(file: FilePart, workspace: Workspace): Mono<StorageProviderResponse>

    fun getId(): String

    @Deprecated("migrate to coroutines")
    fun getDocumentContent(workspace: Workspace, storageLocation: String): Mono<Resource>
}

data class StorageProviderResponse(val storageProviderLocation: String, val sizeInBytes: Long?)