package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
data class Document(
    val name: String,
    val workspaceId: String,
    val timeUploaded: Instant,
    val storageId: String,
    val storageLocation: String?,
    val sizeInBytes: Long?,
    val mimeType: String,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
