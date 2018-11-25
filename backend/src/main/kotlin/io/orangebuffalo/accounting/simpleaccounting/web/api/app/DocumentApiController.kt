package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QDocument
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.*
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/documents")
class DocumentApiController(
    private val extensions: ApiControllersExtensions,
    private val documentService: DocumentService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadNewDocument(
        @PathVariable workspaceId: Long,
        @RequestPart("notes") notes: String?,
        @RequestPart("file") file: Mono<Part>
    ): Mono<DocumentDto> = extensions
        .withAccessibleWorkspace(workspaceId) { workspace ->
            file
                .cast(FilePart::class.java)
                .flatMap { filePart ->
                    documentService.uploadDocument(filePart, notes, workspace)
                }
                .map(::mapDocumentDto)
        }

    @GetMapping
    @PageableApi(DocumentPageableApiDescriptor::class)
    fun getDocuments(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Document>> = extensions.withAccessibleWorkspace(workspaceId) { workspace ->
        documentService.getDocuments(pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{documentId}/content")
    fun getDocumentContent(
        @PathVariable workspaceId: Long,
        @PathVariable documentId: Long
    ): Mono<ResponseEntity<Resource>> = extensions.withAccessibleWorkspace(workspaceId) { workspace ->
        // todo validate access
        documentService.getDocumentById(documentId)
            .flatMap { document ->
                documentService.getDocumentContent(document)
                    .map { fileContent ->
                        ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.name}\"")
                            .contentLength(fileContent.contentLength())
                            .body(fileContent)
                    }
            }
    }
}

data class DocumentDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var timeUploaded: Instant,
    var notes: String?
)

class DocumentPageableApiDescriptor : PageableApiDescriptor<Document, QDocument> {
    override fun mapEntityToDto(entity: Document) = mapDocumentDto(entity)

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
    version = source.version
)
