package io.orangebuffalo.simpleaccounting.domain.documents.storage.local

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import java.io.File
import java.util.*

//todo #82: use IO
@Suppress("EXPERIMENTAL_API_USAGE")
private val localFsStorageContext = newFixedThreadPoolContext(10, "local-fs-storage")

@Service
class LocalFileSystemDocumentsStorage(
    private val config: LocalFileSystemDocumentsStorageProperties
) : DocumentsStorage {

    private val bufferFactory = DefaultDataBufferFactory()

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flow<DataBuffer> {
        return DataBufferUtils.read(
            FileSystemResource(File(config.baseDirectory.toFile(), storageLocation)),
            bufferFactory,
            StreamUtils.BUFFER_SIZE
        ).asFlow()
    }

    override suspend fun getCurrentUserStorageStatus() = DocumentsStorageStatus(true)

    override suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse =
        withContext(localFsStorageContext) {
            val documentDir = File(config.baseDirectory.toFile(), request.workspace.id.toString()).apply { mkdirs() }
            val documentName = "${UUID.randomUUID()}.${File(request.fileName).extension}"
            val documentFile = File(documentDir, documentName)
            documentFile.outputStream().use {
                DataBufferUtils.write(request.content, it)
                    .map { DataBufferUtils.release(it) }
                    .awaitLast()
            }
            val location = documentFile.relativeTo(config.baseDirectory.toFile()).toString()
            SaveDocumentResponse(
                location,
                documentFile.length()
            )
        }

    override fun getId() = "local-fs"

}
