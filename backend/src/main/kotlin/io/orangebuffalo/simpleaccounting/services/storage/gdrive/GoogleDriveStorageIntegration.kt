package io.orangebuffalo.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

// todo #225: unique constraint
@Table
class GoogleDriveStorageIntegration(
    val userId: Long,
    var folderId: String? = null
) : AbstractEntity()
