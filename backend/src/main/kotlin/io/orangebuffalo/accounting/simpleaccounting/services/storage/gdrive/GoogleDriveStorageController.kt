package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GoogleDriveStorageController(
    private val storageService: GoogleDriveDocumentsStorageService
) {

    @RequestMapping("/api/storage/google-drive/status")
    suspend fun getIntegrationStatus(): GoogleDriveStorageIntegrationStatus =
        storageService.getCurrentUserIntegrationStatus()
}