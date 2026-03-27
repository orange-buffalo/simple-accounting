package io.orangebuffalo.simpleaccounting.business.documents.impl

import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageType
import io.orangebuffalo.simpleaccounting.business.documents.DocumentRecord
import io.orangebuffalo.simpleaccounting.business.documents.DocumentStorageStatisticsRecord
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
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

        val usages = mutableListOf<Pair<Long, DocumentUsageGqlDto>>()

        dslContext.select(ea.documentId, ea.expenseId)
            .from(ea)
            .where(ea.documentId.`in`(documentIds))
            .fetch()
            .forEach { r -> usages.add(r[ea.documentId]!! to DocumentUsageGqlDto(DocumentUsageType.EXPENSE, r[ea.expenseId]!!.toInt())) }

        dslContext.select(ia.documentId, ia.incomeId)
            .from(ia)
            .where(ia.documentId.`in`(documentIds))
            .fetch()
            .forEach { r -> usages.add(r[ia.documentId]!! to DocumentUsageGqlDto(DocumentUsageType.INCOME, r[ia.incomeId]!!.toInt())) }

        dslContext.select(iva.documentId, iva.invoiceId)
            .from(iva)
            .where(iva.documentId.`in`(documentIds))
            .fetch()
            .forEach { r -> usages.add(r[iva.documentId]!! to DocumentUsageGqlDto(DocumentUsageType.INVOICE, r[iva.invoiceId]!!.toInt())) }

        dslContext.select(itpa.documentId, itpa.incomeTaxPaymentId)
            .from(itpa)
            .where(itpa.documentId.`in`(documentIds))
            .fetch()
            .forEach { r -> usages.add(r[itpa.documentId]!! to DocumentUsageGqlDto(DocumentUsageType.INCOME_TAX_PAYMENT, r[itpa.incomeTaxPaymentId]!!.toInt())) }

        return usages
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, v) -> v.sortedBy { it.relatedEntityId } }
    }

    override fun countByWorkspaceId(workspaceId: Long): Int = dslContext
        .selectCount()
        .from(document)
        .where(document.workspaceId.eq(workspaceId))
        .fetchOne(0, Int::class.java)!!
}
