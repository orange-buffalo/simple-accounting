package io.orangebuffalo.simpleaccounting.business.documents.storage.local

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider
import io.orangebuffalo.simpleaccounting.infra.inputStreamProvider
import org.springframework.stereotype.Service
import java.io.File
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")

@Service
class LocalFileSystemDocumentsStorage(
    private val config: LocalFileSystemDocumentsStorageProperties,
    private val timeService: TimeService
) : DocumentsStorage {

    override fun getDocumentContent(workspace: Workspace, storageLocation: String): InputStreamProvider =
        inputStreamProvider { File(config.baseDirectory.toFile(), storageLocation).inputStream() }

    override fun deleteDocument(workspace: Workspace, storageLocation: String) {
        File(config.baseDirectory.toFile(), storageLocation).delete()
    }

    override fun getCurrentUserStorageStatus() = DocumentsStorageStatus(true)

    override fun isDownloadAvailableForUser(userId: String) = true

    override fun saveDocument(request: SaveDocumentRequest): SaveDocumentResponse {
        val yearMonth = timeService.currentTime().atZone(ZoneOffset.UTC).format(YEAR_MONTH_FORMATTER)
        val documentDir = File(config.baseDirectory.toFile(), "${request.workspace.id}/$yearMonth").apply { mkdirs() }
        val documentName = "${UUID.randomUUID()}.${File(request.fileName).extension}"
        val documentFile = File(documentDir, documentName)
        documentFile.outputStream().use { outputStream ->
            request.content.useInputStream { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        val location = documentFile.relativeTo(config.baseDirectory.toFile()).toString()
        return SaveDocumentResponse(
            location,
            documentFile.length()
        )
    }

    override fun getId() = "local-fs"

}
