package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.File
import java.util.*

@Service
class LocalFileSystemDocumentStorage(
    private val config: LocalFileSystemDocumentStorageProperties
) : DocumentStorage {

    override fun getDocumentContent(workspace: Workspace, storageLocation: String): Mono<Resource> {
        return Mono.just(FileSystemResource(File(config.baseDirectory.toFile(), storageLocation)))
    }

    override fun saveDocument(file: FilePart, workspace: Workspace): Mono<StorageProviderResponse> {
        return Mono.fromSupplier {
            val documentDir = File(config.baseDirectory.toFile(), workspace.id.toString()).apply { mkdirs() }
            val documentName = "${UUID.randomUUID()}.${File(file.filename()).extension}"
            val documentFile = File(documentDir, documentName)
            file.transferTo(documentFile)
            val location = documentFile.relativeTo(config.baseDirectory.toFile()).toString()
            StorageProviderResponse(location, documentFile.length())
        }
    }

    override fun getId() = "local-fs"

}