package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Google Drive document storage configuration.
 */
@ConfigurationProperties("sa.documents.storage.google-drive")
@Component
data class GoogleDriveDocumentsStorageProperties(
    /**
     * Base URL of the Google Drive API.
     */
    var baseApiUrl: String = "https://www.googleapis.com",

    /**
     * OAuth client id for Google Drive integration.
     */
    var clientId: String = "",

    /**
     * OAuth client secret for Google Drive integration.
     */
    var clientSecret: String = "",
)
