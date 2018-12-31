package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@ConfigurationProperties("simpleaccounting.documents.storage.google-drive")
@Component
class GoogleDriveProperties {
    lateinit var credentialsFile: Resource
    lateinit var dataStoreDirectory: String
    lateinit var redirectUrlBase: String
}
