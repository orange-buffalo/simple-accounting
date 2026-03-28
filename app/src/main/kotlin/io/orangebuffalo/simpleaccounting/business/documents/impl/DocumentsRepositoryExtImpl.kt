package io.orangebuffalo.simpleaccounting.business.documents.impl

import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageType
import io.orangebuffalo.simpleaccounting.business.documents.DocumentRecord
import io.orangebuffalo.simpleaccounting.business.documents.DocumentStorageStatisticsRecord
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.select
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class DocumentsRepositoryExtImpl(
    private val dslContext: DSLContext
) : DocumentsRepositoryExt {

    private val document = Tables.DOCUMENT
    private val workspace = Tables.WORKSPACE
    private val ea = Tables.EXPENSE_ATTACHMENTS
    private val ia = Tables.INCOME_ATTACHMENTS
    private val iva = Tables.INVOICE_ATTACHMENTS
    private val itpa = Tables.INCOME_TAX_PAYMENT_ATTACHMENTS

    override fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long> = dslContext
        .select(document.id)
        .from(document)
        .where(
            document.id.`in`(ids),
            document.workspaceId.eq(workspaceId)
        )
        .fetchInto(Long::class.java)

    override fun getStorageStatsByOwner(ownerId: Long): List<DocumentStorageStatisticsRecord> = dslContext
        .select(document.storageId, count(document.id))
        .from(document)
        .join(workspace).on(workspace.id.eq(document.workspaceId))
        .where(workspace.ownerId.eq(ownerId))
        .groupBy(document.storageId)
        .fetch()
        .map { record ->
            DocumentStorageStatisticsRecord(
                storageId = record.get(document.storageId)!!,
                documentsCount = record.get(count(document.id)),
            )
        }

    override fun findByWorkspaceIdPaginated(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<DocumentRecord> {
        var query = dslContext
            .select(
                document.id, document.version, document.name,
                document.timeUploaded, document.sizeInBytes,
                document.storageId, document.mimeType, document.createdAt,
            )
            .from(document)
            .where(document.workspaceId.eq(workspaceId))

        if (afterCreatedAt != null) {
            query = query.and(document.createdAt.gt(afterCreatedAt))
        }

        return query
            .orderBy(document.createdAt.asc())
            .limit(limit + 1)
            .fetch()
            .map { r ->
                DocumentRecord(
                    id = r[document.id]!!,
                    version = r[document.version]!!,
                    name = r[document.name]!!,
                    timeUploaded = r[document.timeUploaded]!!,
                    sizeInBytes = r[document.sizeInBytes],
                    storageId = r[document.storageId]!!,
                    mimeType = r[document.mimeType]!!,
                    createdAt = r[document.createdAt]!!,
                )
            }
    }

    override fun findUsagesByDocumentIds(documentIds: Collection<Long>): Map<Long, List<DocumentUsageGqlDto>> {
        if (documentIds.isEmpty()) return emptyMap()

        val docIdField = field(name("doc_id"), Long::class.java)
        val entityIdField = field(name("entity_id"), Long::class.java)
        val usageTypeField = field(name("usage_type"), String::class.java)

        return dslContext
            .select(
                ea.documentId.`as`("doc_id"),
                ea.expenseId.`as`("entity_id"),
                inline("EXPENSE").`as`("usage_type"),
            )
            .from(ea)
            .where(ea.documentId.`in`(documentIds))
            .unionAll(
                select(
                    ia.documentId.`as`("doc_id"),
                    ia.incomeId.`as`("entity_id"),
                    inline("INCOME").`as`("usage_type"),
                )
                    .from(ia)
                    .where(ia.documentId.`in`(documentIds))
            )
            .unionAll(
                select(
                    iva.documentId.`as`("doc_id"),
                    iva.invoiceId.`as`("entity_id"),
                    inline("INVOICE").`as`("usage_type"),
                )
                    .from(iva)
                    .where(iva.documentId.`in`(documentIds))
            )
            .unionAll(
                select(
                    itpa.documentId.`as`("doc_id"),
                    itpa.incomeTaxPaymentId.`as`("entity_id"),
                    inline("INCOME_TAX_PAYMENT").`as`("usage_type"),
                )
                    .from(itpa)
                    .where(itpa.documentId.`in`(documentIds))
            )
            .orderBy(docIdField, usageTypeField, entityIdField)
            .fetch()
            .groupBy(
                { r -> r[docIdField]!! },
                { r ->
                    DocumentUsageGqlDto(
                        type = DocumentUsageType.valueOf(r[usageTypeField]!!),
                        relatedEntityId = r[entityIdField]!!.toInt(),
                    )
                }
            )
    }

    override fun countByWorkspaceId(workspaceId: Long): Int = dslContext
        .selectCount()
        .from(document)
        .where(document.workspaceId.eq(workspaceId))
        .fetchOne(0, Int::class.java)!!
}
