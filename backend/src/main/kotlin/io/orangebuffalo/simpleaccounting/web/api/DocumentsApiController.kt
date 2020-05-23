package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.DocumentsService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiPredicateOperator
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
        @RequestPart("file") file: Mono<Part>
    ): DocumentDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        val filePart = file.cast(FilePart::class.java).awaitFirst()
        return documentsService.uploadDocument(filePart, workspace).let(::mapDocumentDto)
    }

    @GetMapping
    suspend fun getDocuments(@PathVariable workspaceId: Long): ApiPage<DocumentDto> =
        filteringApiExecutor.executeFiltering(workspaceId)

    @GetMapping("{documentId}/content")
    suspend fun getDocumentContent(
        @PathVariable workspaceId: Long,
        @PathVariable documentId: Long
    ): ResponseEntity<Flux<DataBuffer>> {
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

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<Document, DocumentDto> {
        query(Tables.DOCUMENT) {
            filterByField("id", Long::class) {
                onPredicate(FilteringApiPredicateOperator.EQ) { documentId ->
                    root.id.eq(documentId)
                }
                onPredicate(FilteringApiPredicateOperator.IN) { documentIds ->
                    root.id.`in`(documentIds)
                }
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

private fun mapDocumentDto(source: Document) = DocumentDto(
    name = source.name,
    timeUploaded = source.timeUploaded,
    id = source.id,
    version = source.version!!,
    sizeInBytes = source.sizeInBytes
)
