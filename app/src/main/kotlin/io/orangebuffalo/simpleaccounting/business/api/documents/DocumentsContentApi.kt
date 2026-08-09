package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/documents")
class DocumentsContentApi(
    private val downloadsService: DownloadsService,
    private val documentsService: DocumentsService,
) {

    @GetMapping("/download/{token}")
    suspend fun getContent(@PathVariable token: String): ResponseEntity<StreamingResponseBody> {
        logger.debug { "Processing document download request" }
        val contentResponse = downloadsService.getContentByToken(token)
        logger.debug {
            "Document download resolved: fileName=${contentResponse.fileName}, " +
                    "contentType=${contentResponse.contentType}, sizeInBytes=${contentResponse.sizeInBytes}"
        }
        val response = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${contentResponse.fileName}\"")
            .contentType(MediaType.parseMediaType(contentResponse.contentType))
        contentResponse.sizeInBytes?.let(response::contentLength)
        return response.body(StreamingResponseBody { outputStream ->
            runBlocking {
                contentResponse.content.collect { buffer ->
                    try {
                        buffer.asInputStream().transferTo(outputStream)
                    } finally {
                        DataBufferUtils.release(buffer)
                    }
                }
            }
        })
    }

    @PostMapping("/upload/{token}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadDocument(
        @PathVariable token: String,
        @RequestPart("file") file: MultipartFile,
    ): DocumentGqlDto {
        val document = documentsService.saveDocumentByUploadToken(
            token = token,
            fileName = file.originalFilename ?: "",
            content = DataBufferUtils.readInputStream(
                file::getInputStream,
                DefaultDataBufferFactory(),
                StreamUtils.BUFFER_SIZE,
            ),
            contentType = file.contentType,
        )
        return document.toGqlDto()
    }

    @ExceptionHandler
    fun onEntityNotFoundException(exception: EntityNotFoundException): ResponseEntity<String> {
        logger.trace(exception) {}
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.message)
    }
}
