package io.orangebuffalo.simpleaccounting.services.storage.local

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.services.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.services.storage.StorageProviderResponse
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import reactor.core.publisher.Flux
import java.io.File
import java.util.*

//todo #82: use IO
@Suppress("EXPERIMENTAL_API_USAGE")
private val localFsStorageContext = newFixedThreadPoolContext(10, "local-fs-storage")

@Service
class LocalFileSystemDocumentStorage(
    private val config: LocalFileSystemDocumentStorageProperties
) : DocumentsStorage {

    private val bufferFactory = DefaultDataBufferFactory()

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer> {
        return DataBufferUtils.read(
            FileSystemResource(File(config.baseDirectory.toFile(), storageLocation)),
            bufferFactory,
            StreamUtils.BUFFER_SIZE
        )
    }

    override suspend fun saveDocument(request: SaveDocumentRequest): StorageProviderResponse =
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
            StorageProviderResponse(
                location,
                documentFile.length()
            )
        }

    override fun getId() = "local-fs"

}
