package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.ZonedDateTime

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
}

data class DocumentDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var dateUploaded: ZonedDateTime,
    var notes: String?
)

private fun mapDocumentDto(source: Document) = DocumentDto(
    name = source.name,
    dateUploaded = source.dateUploaded,
    notes = source.notes,
    id = source.id,
    version = source.version
)
