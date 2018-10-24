package io.orangebuffalo.accounting.simpleaccounting.services.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Path

@ConfigurationProperties(prefix = "simpleaccounting.documents.storage.local-fs")
@Component
class LocalFileSystemDocumentStorageProperties {

    lateinit var baseDirectory: Path
}