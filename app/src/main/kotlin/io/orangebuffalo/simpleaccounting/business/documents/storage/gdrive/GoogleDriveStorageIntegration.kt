package io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
data class GoogleDriveStorageIntegration(
    val userId: String,
    val folderId: String? = null,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
