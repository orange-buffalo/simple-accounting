package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/documents")
class DocumentsContentApi(
    private val downloadsService: DownloadsService,
    private val documentsService: DocumentsService,
) {

    @GetMapping("/download/{token}")
    suspend fun getContent(@PathVariable token: String): ResponseEntity<Flow<DataBuffer>> {
        logger.debug { "Processing document download request" }
        val contentResponse = downloadsService.getContentByToken(token)
        logger.debug {
            "Document download resolved: fileName=${contentResponse.fileName}, " +
                    "contentType=${contentResponse.contentType}, sizeInBytes=${contentResponse.sizeInBytes}"
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${contentResponse.fileName}\"")
            .contentLength(contentResponse.sizeInBytes ?: -1)
            .contentType(MediaType.parseMediaType(contentResponse.contentType))
            .body(contentResponse.content)
    }

    @PostMapping("/upload/{token}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadDocument(
        @PathVariable token: String,
        @RequestPart("file") filePart: FilePart,
    ): DocumentGqlDto {
        val document = documentsService.saveDocumentByUploadToken(
            token = token,
            fileName = filePart.filename(),
            content = filePart.content(),
            contentType = filePart.headers().contentType?.toString(),
        )
        return document.toGqlDto()
    }

    @ExceptionHandler
    fun onEntityNotFoundException(exception: EntityNotFoundException): Mono<ResponseEntity<String>> {
        logger.trace(exception) {}
        return Mono.just(
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.message)
        )
    }
}
