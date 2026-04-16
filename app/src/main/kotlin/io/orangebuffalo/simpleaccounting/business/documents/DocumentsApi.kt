package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/documents")
class DocumentsApi(
    private val documentsService: DocumentsService,
    private val workspacesService: WorkspacesService,
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadNewDocument(
        @PathVariable workspaceId: Long,
        @RequestPart("file") filePart: FilePart
    ): DocumentDto {
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        return documentsService
            .saveDocument(
                SaveDocumentRequest(
                    fileName = filePart.filename(),
                    workspace = workspace,
                    content = filePart.content(),
                    contentType = filePart.headers().contentType?.toString()
                )
            )
            .let(::mapDocumentDto)
    }
}

data class DocumentDto(
    var id: Long,
    var version: Int,
    var name: String,
    var timeUploaded: Instant,
    var sizeInBytes: Long?,
    var storageId: String,
    var mimeType: String,
)

private fun mapDocumentDto(source: Document) =
    DocumentDto(
        name = source.name,
        timeUploaded = source.timeUploaded,
        id = source.id!!,
        version = source.version!!,
        sizeInBytes = source.sizeInBytes,
        storageId = source.storageId,
        mimeType = source.mimeType,
    )
