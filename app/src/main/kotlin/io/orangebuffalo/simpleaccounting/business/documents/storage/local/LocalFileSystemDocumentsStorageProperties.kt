package io.orangebuffalo.simpleaccounting.business.documents.storage.local

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Path

@ConfigurationProperties(prefix = "simpleaccounting.documents.storage.local-fs")
@Component
class LocalFileSystemDocumentsStorageProperties {

    lateinit var baseDirectory: Path
}
