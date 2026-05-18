package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("DOCUMENTS_MIGRATION")
data class DocumentsMigration(
    val userId: String,
    @field:MappedCollection(idColumn = "MIGRATION_ID")
    val documentsToMigrate: Set<DocumentsMigrationDocument> = setOf(),
    val migratedDocumentsCount: Int = 0,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

@Table("DOCUMENTS_MIGRATION_DOCUMENT")
data class DocumentsMigrationDocument(
    val documentId: String,
)
