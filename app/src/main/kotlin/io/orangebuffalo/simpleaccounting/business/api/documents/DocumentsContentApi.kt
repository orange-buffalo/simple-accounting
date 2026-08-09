package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.infra.inputStreamProvider
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
            contentResponse.content.useInputStream { inputStream ->
                inputStream.transferTo(outputStream)
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
            content = inputStreamProvider(file::getInputStream),
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
