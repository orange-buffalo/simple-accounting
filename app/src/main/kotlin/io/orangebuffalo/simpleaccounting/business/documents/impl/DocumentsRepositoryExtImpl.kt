package io.orangebuffalo.simpleaccounting.business.documents.impl

import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageGqlDto
import io.orangebuffalo.simpleaccounting.business.api.DocumentUsageType
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
    private val expense = Tables.EXPENSE
    private val income = Tables.INCOME
    private val invoice = Tables.INVOICE
    private val incomeTaxPayment = Tables.INCOME_TAX_PAYMENT

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

    override fun findUsagesByDocumentIds(documentIds: Collection<Long>): Map<Long, List<DocumentUsageGqlDto>> {
        if (documentIds.isEmpty()) return emptyMap()

        val docIdField = field(name("doc_id"), Long::class.java)
        val entityIdField = field(name("entity_id"), Long::class.java)
        val usageTypeField = field(name("usage_type"), String::class.java)
        val displayNameField = field(name("display_name"), String::class.java)

        return dslContext
            .select(
                ea.documentId.`as`("doc_id"),
                ea.expenseId.`as`("entity_id"),
                inline("EXPENSE").`as`("usage_type"),
                expense.title.`as`("display_name"),
            )
            .from(ea)
            .join(expense).on(expense.id.eq(ea.expenseId))
            .where(ea.documentId.`in`(documentIds))
            .unionAll(
                select(
                    ia.documentId.`as`("doc_id"),
                    ia.incomeId.`as`("entity_id"),
                    inline("INCOME").`as`("usage_type"),
                    income.title.`as`("display_name"),
                )
                    .from(ia)
                    .join(income).on(income.id.eq(ia.incomeId))
                    .where(ia.documentId.`in`(documentIds))
            )
            .unionAll(
                select(
                    iva.documentId.`as`("doc_id"),
                    iva.invoiceId.`as`("entity_id"),
                    inline("INVOICE").`as`("usage_type"),
                    invoice.title.`as`("display_name"),
                )
                    .from(iva)
                    .join(invoice).on(invoice.id.eq(iva.invoiceId))
                    .where(iva.documentId.`in`(documentIds))
            )
            .unionAll(
                select(
                    itpa.documentId.`as`("doc_id"),
                    itpa.incomeTaxPaymentId.`as`("entity_id"),
                    inline("INCOME_TAX_PAYMENT").`as`("usage_type"),
                    incomeTaxPayment.title.`as`("display_name"),
                )
                    .from(itpa)
                    .join(incomeTaxPayment).on(incomeTaxPayment.id.eq(itpa.incomeTaxPaymentId))
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
                        displayName = r[displayNameField]!!,
                    )
                }
            )
    }
}
