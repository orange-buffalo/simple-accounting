package io.orangebuffalo.simpleaccounting.domain.documents.storage.noop

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.security.util.InMemoryResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

@Service
class NoopDocumentsStorage : DocumentsStorage {
    private val bufferFactory = DefaultDataBufferFactory()

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

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Flux<DataBuffer> {
        val resource = getFakeContent(storageLocation)
        val contentLength = resource.contentLength()
        val bufferSize = max(1, contentLength / 30)
        return DataBufferUtils
            .read(
                resource,
                bufferFactory,
                bufferSize.toInt()
            )
            .asFlow()
            // simulate some storage delay
            .map { dataBuffer ->
                delay(100)
                dataBuffer
            }
            .asFlux()
    }

    override suspend fun getCurrentUserStorageStatus() = DocumentsStorageStatus(true)
}
