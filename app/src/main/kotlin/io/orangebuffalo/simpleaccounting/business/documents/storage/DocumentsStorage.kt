package io.orangebuffalo.simpleaccounting.business.documents.storage

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider

interface DocumentsStorage {

    fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse

    fun getId(): String

    fun getDocumentContent(workspace: Workspace, storageLocation: String): InputStreamProvider

    fun deleteDocument(workspace: Workspace, storageLocation: String)

    fun getCurrentUserStorageStatus(): DocumentsStorageStatus

    fun isDownloadAvailableForUser(userId: String): Boolean
}

data class SaveDocumentResponse(val storageLocation: String, val sizeInBytes: Long?)

data class SaveDocumentRequest(
    val fileName: String,
    val content: InputStreamProvider,
    val workspace: Workspace,
    val contentType: String? = null
)

open class DocumentStorageException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class StorageAuthorizationRequiredException(message: String? = null, cause: Throwable? = null)
    : DocumentStorageException(message, cause)

data class DocumentsStorageStatus(
    val active: Boolean
)
