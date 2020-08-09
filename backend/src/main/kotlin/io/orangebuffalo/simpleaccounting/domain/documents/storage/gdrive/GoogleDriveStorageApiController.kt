package io.orangebuffalo.simpleaccounting.domain.documents.storage.gdrive

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GoogleDriveStorageApiController(
    private val storage: GoogleDriveDocumentsStorage
) {

    @RequestMapping("/api/storage/google-drive/status")
    suspend fun getIntegrationStatus(): GoogleDriveStorageIntegrationStatus =
        storage.getCurrentUserIntegrationStatus()
}
