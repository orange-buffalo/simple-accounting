package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.accounting.simpleaccounting.services.integration.toMono
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class GoogleDriveStorageController(
    private val storageService: GoogleDriveDocumentStorageService
) {

    @RequestMapping("/api/storage/google-drive/status")
    fun getIntegrationStatus(): Mono<GoogleDriveStorageIntegrationStatus> = toMono {
        storageService.getCurrentUserIntegrationStatus()
    }
}