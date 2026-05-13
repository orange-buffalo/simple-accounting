package io.orangebuffalo.simpleaccounting.business.standalonedocuments

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Represents a user-managed document entry that exists independently from incomes, expenses, invoices, or tax payments.
 *
 * The entity stores the user-facing title and references the uploaded [io.orangebuffalo.simpleaccounting.business.documents.Document]
 * that contains the actual file metadata and storage location.
 */
@Table
data class StandaloneDocument(
    val title: String,
    val documentId: String,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
