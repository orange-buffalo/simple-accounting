package io.orangebuffalo.simpleaccounting.business.documents.impl

import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageType
import io.orangebuffalo.simpleaccounting.business.documents.DocumentStorageStatisticsRecord
import io.orangebuffalo.simpleaccounting.business.documents.DocumentWithUsagesRecord
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

    override fun findByWorkspaceIdPaginatedWithUsages(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<DocumentWithUsagesRecord> {
        var docQuery = dslContext
            .select(*document.fields())
            .from(document)
            .where(document.workspaceId.eq(workspaceId))

        if (afterCreatedAt != null) {
            docQuery = docQuery.and(document.createdAt.gt(afterCreatedAt))
        }

        val docPage = docQuery
            .orderBy(document.createdAt.asc())
            .limit(limit + 1)
            .asTable("doc_page")

        val docPageId = docPage.field(document.id)!!
        val docPageCreatedAt = docPage.field(document.createdAt)!!

        val records = dslContext
            .select(
                docPage.fields().toList()
                    + listOf(ea.expenseId, ia.incomeId, iva.invoiceId, itpa.incomeTaxPaymentId)
            )
            .from(docPage)
            .leftJoin(ea).on(ea.documentId.eq(docPageId))
            .leftJoin(ia).on(ia.documentId.eq(docPageId))
            .leftJoin(iva).on(iva.documentId.eq(docPageId))
            .leftJoin(itpa).on(itpa.documentId.eq(docPageId))
            .orderBy(docPageCreatedAt.asc())
            .fetch()

        return records.groupBy { it[docPageId]!! }
            .entries
            .map { (_, rowGroup) ->
                val first = rowGroup.first()
                val usages = rowGroup.flatMap { row ->
                    listOfNotNull(
                        row[ea.expenseId]?.let {
                            DocumentUsageGqlDto(DocumentUsageType.EXPENSE, it.toInt())
                        },
                        row[ia.incomeId]?.let {
                            DocumentUsageGqlDto(DocumentUsageType.INCOME, it.toInt())
                        },
                        row[iva.invoiceId]?.let {
                            DocumentUsageGqlDto(DocumentUsageType.INVOICE, it.toInt())
                        },
                        row[itpa.incomeTaxPaymentId]?.let {
                            DocumentUsageGqlDto(DocumentUsageType.INCOME_TAX_PAYMENT, it.toInt())
                        },
                    )
                }.distinct()

                DocumentWithUsagesRecord(
                    id = first[docPageId]!!,
                    version = first[docPage.field(document.version)!!]!!,
                    name = first[docPage.field(document.name)!!]!!,
                    timeUploaded = first[docPage.field(document.timeUploaded)!!]!!,
                    sizeInBytes = first[docPage.field(document.sizeInBytes)!!],
                    storageId = first[docPage.field(document.storageId)!!]!!,
                    mimeType = first[docPage.field(document.mimeType)!!]!!,
                    createdAt = first[docPageCreatedAt]!!,
                    usages = usages,
                )
            }
    }

    override fun countByWorkspaceId(workspaceId: Long): Int = dslContext
        .selectCount()
        .from(document)
        .where(document.workspaceId.eq(workspaceId))
        .fetchOne(0, Int::class.java)!!
}
