package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentsService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.awaitMono
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QDocument
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.*
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.domain.Page
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/documents")
class DocumentsApiController(
    private val extensions: ApiControllersExtensions,
    private val documentsService: DocumentsService,
    private val workspaceService: WorkspaceService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadNewDocument(
        @PathVariable workspaceId: Long,
        @RequestPart(name = "notes", required = false) notes: String?,
        @RequestPart("file") file: Mono<Part>
    ): Mono<DocumentDto> = extensions.toMono {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        val filePart = file.cast(FilePart::class.java).awaitMono()
        documentsService.uploadDocument(filePart, notes, workspace).let(::mapDocumentDto)
    }

    @GetMapping
    @PageableApi(DocumentPageableApiDescriptor::class)
    fun getDocuments(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Document>> = extensions.toMono {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        documentsService.getDocuments(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{documentId}/content")
    fun getDocumentContent(
        @PathVariable workspaceId: Long,
        @PathVariable documentId: Long
    ): Mono<ResponseEntity<Flux<DataBuffer>>> = extensions.toMono {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val document = documentsService.getDocumentByIdAndWorkspace(documentId, workspace)
            ?: throw EntityNotFoundException("Document $documentId is not found")

        documentsService.getDocumentContent(document)
            .let { fileContent ->
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.name}\"")
                    .contentLength(document.sizeInBytes ?: -1)
                    .body(fileContent)
            }
    }
}

data class DocumentDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var timeUploaded: Instant,
    var notes: String?,
    var sizeInBytes: Long?
)

@Component
class DocumentPageableApiDescriptor : PageableApiDescriptor<Document, QDocument> {
    override suspend fun mapEntityToDto(entity: Document) = mapDocumentDto(entity)

    override fun getSupportedFilters(): List<PageableApiFilter<Document, QDocument>> = apiFilters(QDocument.document) {
        byApiField("id", Long::class) {
            onOperator(PageableApiFilterOperator.EQ) { value -> id.eq(value) }
        }
    }
}

private fun mapDocumentDto(source: Document) = DocumentDto(
    name = source.name,
    timeUploaded = source.timeUploaded,
    notes = source.notes,
    id = source.id,
    version = source.version,
    sizeInBytes = source.sizeInBytes
)
