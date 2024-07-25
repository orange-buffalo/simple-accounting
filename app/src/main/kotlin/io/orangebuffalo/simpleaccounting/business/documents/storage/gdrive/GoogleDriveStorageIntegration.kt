package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

@Table
class GoogleDriveStorageIntegration(
    val userId: Long,
    var folderId: String? = null
) : AbstractEntity()
