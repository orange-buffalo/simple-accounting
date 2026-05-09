package io.orangebuffalo.simpleaccounting.business.documents.storage.local

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Local filesystem document storage configuration.
 */
@ConfigurationProperties(prefix = "sa.documents.storage.local-fs")
@Component
class LocalFileSystemDocumentsStorageProperties {

    /**
     * Whether local filesystem document storage is enabled.
     */
    var enabled: Boolean = false

    /**
     * Directory where documents are stored on the local filesystem.
     */
    lateinit var baseDirectory: Path
}
