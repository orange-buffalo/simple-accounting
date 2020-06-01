package io.orangebuffalo.simpleaccounting.services.storage

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux

interface DocumentsStorage {

    suspend fun saveDocument(request: SaveDocumentRequest): StorageProviderResponse

    fun getId(): String

    suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer>
}

data class StorageProviderResponse(val storageProviderLocation: String, val sizeInBytes: Long?)

data class SaveDocumentRequest(
    val fileName: String,
    val content: Flux<DataBuffer>,
    val workspace: Workspace
)

open class DocumentStorageException(message: String? = null) : RuntimeException(message)

class StorageAuthorizationRequiredException : DocumentStorageException()
