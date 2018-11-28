package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.core.io.Resource
import org.springframework.http.codec.multipart.FilePart

interface DocumentStorage {

    suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse

    fun getId(): String

    suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Resource
}

data class StorageProviderResponse(val storageProviderLocation: String, val sizeInBytes: Long?)