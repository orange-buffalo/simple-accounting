package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux

interface DocumentsStorage {

    suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse

    fun getId(): String

    suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer>
}

data class StorageProviderResponse(val storageProviderLocation: String, val sizeInBytes: Long?)

class StorageAuthorizationRequiredException : RuntimeException()