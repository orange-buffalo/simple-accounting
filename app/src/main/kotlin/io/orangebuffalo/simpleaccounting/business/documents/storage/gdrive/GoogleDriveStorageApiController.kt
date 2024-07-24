package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GoogleDriveStorageApiController(
    private val storage: GoogleDriveDocumentsStorage
) {

    @GetMapping("/api/storage/google-drive/status")
    suspend fun getIntegrationStatus(): GoogleDriveStorageIntegrationStatus =
        storage.getCurrentUserIntegrationStatus()
}
