package io.orangebuffalo.simpleaccounting.services.storage.local

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.services.storage.StorageProviderResponse
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.codec.multipart.FilePart
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

    override suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse =
        withContext(localFsStorageContext) {
            val documentDir = File(config.baseDirectory.toFile(), workspace.id.toString()).apply { mkdirs() }
            val documentName = "${UUID.randomUUID()}.${File(file.filename()).extension}"
            val documentFile = File(documentDir, documentName)
            file.transferTo(documentFile).awaitFirstOrNull()
            val location = documentFile.relativeTo(config.baseDirectory.toFile()).toString()
            StorageProviderResponse(
                location,
                documentFile.length()
            )
        }

    override fun getId() = "local-fs"

}
