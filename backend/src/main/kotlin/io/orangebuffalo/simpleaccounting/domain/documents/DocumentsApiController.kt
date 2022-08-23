package io.orangebuffalo.simpleaccounting.domain.documents

import io.orangebuffalo.simpleaccounting.domain.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.*
import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.Flow
import org.springdoc.api.annotations.ParameterObject
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/documents")
class DocumentsApiController(
    private val documentsService: DocumentsService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadNewDocument(
        @PathVariable workspaceId: Long,
        @RequestPart("file") filePart: FilePart
    ): DocumentDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        return documentsService
            .saveDocument(
                SaveDocumentRequest(
                    fileName = filePart.filename(),
                    workspace = workspace,
                    content = filePart.content()
                )
            )
            .let(::mapDocumentDto)
    }

    @GetMapping
    suspend fun getDocuments(
        @PathVariable workspaceId: Long,
        @ParameterObject request: DocumentsFilteringRequest
    ): ApiPage<DocumentDto> =
        filteringApiExecutor.executeFiltering(request, workspaceId)

    @GetMapping("{documentId}/content")
    suspend fun getDocumentContent(
        @PathVariable workspaceId: Long,
        @PathVariable documentId: Long
    ): ResponseEntity<Flow<DataBuffer>> {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val document = documentsService.getDocumentByIdAndWorkspaceId(documentId, workspaceId)
            ?: throw EntityNotFoundException("Document $documentId is not found")

        return documentsService.getDocumentContent(document)
            .let { fileContent ->
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.name}\"")
                    .contentLength(document.sizeInBytes ?: -1)
                    .body(fileContent)
            }
    }

    @GetMapping("{documentId}/download-token")
    suspend fun getDownloadToken(
        @PathVariable workspaceId: Long,
        @PathVariable documentId: Long
    ): GetDownloadTokenResponse = GetDownloadTokenResponse(
        token = documentsService.getDownloadToken(workspaceId, documentId)
    )

    private val filteringApiExecutor = filteringApiExecutorBuilder
        .executor<Document, DocumentDto, NoOpSorting, DocumentsFilteringRequest> {
            query(Tables.DOCUMENT) {
                onFilter(DocumentsFilteringRequest::idIn) { ids ->
                    root.id.`in`(ids)
                }
                onFilter(DocumentsFilteringRequest::idEq) { id ->
                    root.id.eq(id)
                }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
                addDefaultSorting { root.id.desc() }
            }
            mapper { mapDocumentDto(this) }
        }
}

data class DocumentDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var timeUploaded: Instant,
    var sizeInBytes: Long?
)

private fun mapDocumentDto(source: Document) =
    DocumentDto(
        name = source.name,
        timeUploaded = source.timeUploaded,
        id = source.id,
        version = source.version!!,
        sizeInBytes = source.sizeInBytes
    )

data class GetDownloadTokenResponse(val token: String)

class DocumentsFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null

    @field:Parameter(name = "id[in]")
    var idIn: List<Long>? = null

    @field:Parameter(name = "id[eq]")
    var idEq: Long? = null
}
