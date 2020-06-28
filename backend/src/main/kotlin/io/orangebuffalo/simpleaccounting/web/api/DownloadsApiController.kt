package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.integration.downloads.DownloadsService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/downloads")
class DownloadsApiController(
    private val downloadsService: DownloadsService
) {

    @GetMapping(params = ["token"])
    suspend fun getContent(@RequestParam token: String): ResponseEntity<Flux<DataBuffer>> {
        val contentResponse = downloadsService.getContentByToken(token)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${contentResponse.fileName}\"")
            .contentLength(contentResponse.sizeInBytes ?: -1)
            .body(contentResponse.content)
    }
}
