package io.orangebuffalo.simpleaccounting.business.standalonedocuments

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
data class StandaloneDocument(
    val title: String,
    val documentId: String,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
