package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.services.persistence.entities.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
class Document(
    var name: String,
    val workspaceId: Long,
    val timeUploaded: Instant,
    var storageId: String,
    var storageLocation: String?,
    val sizeInBytes: Long?
) : AbstractEntity()
