package io.orangebuffalo.simpleaccounting.services.storage.noop

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.services.storage.StorageProviderResponse
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.util.InMemoryResource
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import reactor.core.publisher.Flux
import kotlin.random.Random

@Service
class NoopDocumentsStorageService : DocumentsStorage {
    private val bufferFactory = DefaultDataBufferFactory()

    override suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse {
        val filename = file.filename()
        if (filename.contains("fail")) {
            throw RuntimeException("Upload failed")
        }
        return StorageProviderResponse(filename, Random.nextLong(42, 42_000_000))
    }

    override fun getId(): String = "noop"

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer> {
        val resource = InMemoryResource(storageLocation)
        return DataBufferUtils.read(
            resource,
            bufferFactory,
            StreamUtils.BUFFER_SIZE
        )
    }
}
