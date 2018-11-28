package io.orangebuffalo.accounting.simpleaccounting.services.storage

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

private val localFsStorageContext = newFixedThreadPoolContext(10, "local-fs-storage")

@Service
class LocalFileSystemDocumentStorage(
    private val config: LocalFileSystemDocumentStorageProperties
) : DocumentStorage {

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Resource {
        return FileSystemResource(File(config.baseDirectory.toFile(), storageLocation))
    }

    override suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse =
        withContext(localFsStorageContext) {
            val documentDir = File(config.baseDirectory.toFile(), workspace.id.toString()).apply { mkdirs() }
            val documentName = "${UUID.randomUUID()}.${File(file.filename()).extension}"
            val documentFile = File(documentDir, documentName)
            file.transferTo(documentFile).awaitFirstOrNull()
            val location = documentFile.relativeTo(config.baseDirectory.toFile()).toString()
            StorageProviderResponse(location, documentFile.length())
        }

    override fun getId() = "local-fs"

}