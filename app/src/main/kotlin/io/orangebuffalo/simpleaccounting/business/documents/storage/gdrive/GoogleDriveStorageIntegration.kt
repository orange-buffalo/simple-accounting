package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class GoogleDriveStorageIntegration(
    val userId: Long,
    var folderId: String? = null
) : AbstractEntity()
