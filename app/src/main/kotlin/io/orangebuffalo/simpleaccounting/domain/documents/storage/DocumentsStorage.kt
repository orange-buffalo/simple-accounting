package io.orangebuffalo.simpleaccounting.domain.documents.storage

import io.orangebuffalo.simpleaccounting.domain.workspaces.Workspace
import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux

interface DocumentsStorage {

    suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse

    fun getId(): String

    suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flow<DataBuffer>

    suspend fun getCurrentUserStorageStatus(): DocumentsStorageStatus
}

data class SaveDocumentResponse(val storageLocation: String, val sizeInBytes: Long?)

data class SaveDocumentRequest(
    val fileName: String,
    val content: Flux<DataBuffer>,
    val workspace: Workspace
)

open class DocumentStorageException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class StorageAuthorizationRequiredException(message: String? = null, cause: Throwable? = null)
    : DocumentStorageException(message, cause)

data class DocumentsStorageStatus(
    val active: Boolean
)
