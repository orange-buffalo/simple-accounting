package io.orangebuffalo.simpleaccounting.tests.infra.ui

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider
import io.orangebuffalo.simpleaccounting.infra.inputStreamProvider
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class TestDocumentsStorage : DocumentsStorage {
    private val uploadedDocuments: MutableMap<String, ByteArray> = ConcurrentHashMap()
    private var storageStatus = DocumentsStorageStatus(active = true)

    override fun getId(): String = STORAGE_ID

    companion object {
        const val STORAGE_ID = "test-storage"
    }

    override fun getCurrentUserStorageStatus(): DocumentsStorageStatus = storageStatus

    override fun isDownloadAvailableForUser(userId: String) = true

    override fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse {
        val content = request.content.useInputStream { it.readBytes() }
        val storageLocation = UUID.randomUUID().toString()
        uploadedDocuments[storageLocation] = content
        return SaveDocumentResponse(
            storageLocation = storageLocation,
            sizeInBytes = content.size.toLong()
        )
    }

    override fun getDocumentContent(workspace: Workspace, storageLocation: String): InputStreamProvider {
        val content = uploadedDocuments[storageLocation]
            ?: throw IllegalStateException("No content found for location: $storageLocation")
        return inputStreamProvider(content::inputStream)
    }

    override fun deleteDocument(workspace: Workspace, storageLocation: String) {
        uploadedDocuments.remove(storageLocation)
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

    fun hasUploadedContent(storageLocation: String): Boolean = uploadedDocuments.containsKey(storageLocation)

    fun reset() {
        uploadedDocuments.clear()
        storageStatus = DocumentsStorageStatus(active = true)
    }
}
