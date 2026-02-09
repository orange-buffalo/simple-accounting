package io.orangebuffalo.simpleaccounting.tests.infra.ui

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.*

@Component
class TestDocumentsStorage : DocumentsStorage {
    private val uploadedDocuments = mutableMapOf<String, ByteArray>()
    private var storageStatus = DocumentsStorageStatus(active = true)

    override fun getId(): String = STORAGE_ID

    companion object {
        const val STORAGE_ID = "test-storage"
    }

    override suspend fun getCurrentUserStorageStatus(): DocumentsStorageStatus = storageStatus

    override suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse {
        val os = ByteArrayOutputStream()
        DataBufferUtils.write(request.content, os)
            .map { DataBufferUtils.release(it) }
            .awaitLast()
        val content = os.toByteArray()
        val storageLocation = UUID.randomUUID().toString()
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
