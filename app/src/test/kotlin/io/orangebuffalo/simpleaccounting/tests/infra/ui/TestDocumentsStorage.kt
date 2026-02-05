package io.orangebuffalo.simpleaccounting.tests.infra.ui

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.utils.consumeToString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component

@Component
class TestDocumentsStorage : DocumentsStorage {
    private val uploadedDocuments = mutableMapOf<String, ByteArray>()
    private var storageStatus = DocumentsStorageStatus(active = true)

    override fun getId(): String = "test-storage"

    override suspend fun getCurrentUserStorageStatus(): DocumentsStorageStatus = storageStatus

    override suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse {
        val content = request.content.asFlow().consumeToString().toByteArray()
        val storageLocation = "storage-location-${uploadedDocuments.size + 1}"
        uploadedDocuments[storageLocation] = content
        return SaveDocumentResponse(
            storageLocation = storageLocation,
            sizeInBytes = content.size.toLong()
        )
    }

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flow<DataBuffer> {
        val content = uploadedDocuments[storageLocation]
            ?: throw IllegalStateException("No content found for location: $storageLocation")
        return flowOf(DefaultDataBufferFactory.sharedInstance.wrap(content))
    }

    fun mockDocumentContent(storageLocation: String, content: ByteArray) {
        uploadedDocuments[storageLocation] = content
    }

    fun setStorageStatus(active: Boolean) {
        storageStatus = DocumentsStorageStatus(active = active)
    }

    fun getUploadedContent(storageLocation: String): ByteArray {
        return uploadedDocuments[storageLocation]
            ?: throw IllegalStateException("No content found for location: $storageLocation")
    }

    fun reset() {
        uploadedDocuments.clear()
        storageStatus = DocumentsStorageStatus(active = true)
    }
}
