package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.File
import java.util.*

@Service
class LocalFileSystemDocumentStorage(
    private val config: LocalFileSystemDocumentStorageProperties
) : DocumentStorage {

    override fun saveDocument(file: FilePart, workspace: Workspace): Mono<String> {
        return Mono.fromSupplier {
            val documentDir = File(config.baseDirectory.toFile(), workspace.id.toString()).apply { mkdirs() }
            val documentName = "${UUID.randomUUID()}.${File(file.filename()).extension}"
            val documentFile = File(documentDir, documentName)
            file.transferTo(documentFile)
            documentFile.relativeTo(config.baseDirectory.toFile()).toString()
        }
    }

    override fun getId() = "local-fs"

}