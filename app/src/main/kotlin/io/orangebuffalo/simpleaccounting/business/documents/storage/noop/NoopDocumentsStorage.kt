package io.orangebuffalo.simpleaccounting.business.documents.storage.noop

import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider
import io.orangebuffalo.simpleaccounting.infra.inputStreamProvider
import org.springframework.security.util.InMemoryResource
import org.springframework.stereotype.Service
import java.io.FilterInputStream
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

@Service
class NoopDocumentsStorage : DocumentsStorage {
    override suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse {
        val filename = request.fileName
        if (filename.contains("fail")) {
            throw RuntimeException("Upload failed")
        }
        return SaveDocumentResponse(filename, getFakeContent(filename).contentLength())
    }

    private fun getFakeContent(filename:String) : InMemoryResource  {
        return InMemoryResource(
            "START-// ${filename.repeat(
                ThreadLocalRandom.current().nextInt(20_000, 30_000)
            )} //-END"
        )
    }

    override fun getId(): String = "noop"

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): InputStreamProvider {
        val resource = getFakeContent(storageLocation)
        val contentLength = resource.contentLength()
        val bufferSize = max(1, contentLength / 30)
        return inputStreamProvider {
            object : FilterInputStream(resource.inputStream) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    super.read(buffer, offset, minOf(length, bufferSize.toInt()))
                        .also { bytesRead ->
                            if (bytesRead > 0) Thread.sleep(100)
                        }

                override fun read(): Int = super.read().also { value ->
                    if (value >= 0) Thread.sleep(100)
                }
            }
        }
    }

    override suspend fun deleteDocument(workspace: Workspace, storageLocation: String) {
    }

    override suspend fun getCurrentUserStorageStatus() = DocumentsStorageStatus(true)

    override suspend fun isDownloadAvailableForUser(userId: String) = true
}
