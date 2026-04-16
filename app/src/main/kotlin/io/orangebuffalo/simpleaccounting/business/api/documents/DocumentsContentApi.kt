package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/documents")
class DocumentsContentApi(
    private val downloadsService: DownloadsService,
    private val documentsService: DocumentsService,
) {

    @GetMapping("/download/{token}")
    suspend fun getContent(@PathVariable token: String): ResponseEntity<Flow<DataBuffer>> {
        val contentResponse = downloadsService.getContentByToken(token)
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
}
