package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.select
import org.springframework.stereotype.Component

@Component
class DocumentsGqlApi(
    private val paginationService: GraphqlPaginationService,
    private val documentsRepository: DocumentsRepository,
) {

    private val document = Tables.DOCUMENT
    private val expenseAttachments = Tables.EXPENSE_ATTACHMENTS
    private val incomeAttachments = Tables.INCOME_ATTACHMENTS
    private val invoiceAttachments = Tables.INVOICE_ATTACHMENTS
    private val incomeTaxPaymentAttachments = Tables.INCOME_TAX_PAYMENT_ATTACHMENTS
    private val standaloneDocument = Tables.STANDALONE_DOCUMENT
    private val expense = Tables.EXPENSE
    private val income = Tables.INCOME
    private val invoice = Tables.INVOICE
    private val incomeTaxPayment = Tables.INCOME_TAX_PAYMENT

    suspend fun loadDocuments(
        workspaceId: String,
        first: Int,
        after: String?,
        freeSearchText: String?,
        storageIdsIn: List<String>?,
        usageTypeIn: List<DocumentUsageFilterType>?,
    ): ConnectionGqlDto<DocumentGqlDto> {
        val usageSummary = DocumentUsageSummary(freeSearchText)

        return paginationService.forTable(document)
            .onQuery { query ->
                query.leftJoin(usageSummary.table).on(usageSummary.documentId.eq(document.id))
            }
            .addPredicate(document.workspaceId.eq(workspaceId))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(
                        DSL.or(
                            document.name.containsIgnoreCase(freeSearchText),
                            usageSummary.matchingTitleUsages.greaterThan(0),
                        )
                    )
                }
                if (!storageIdsIn.isNullOrEmpty()) {
                    it.addPredicate(document.storageId.`in`(storageIdsIn))
                }
                if (!usageTypeIn.isNullOrEmpty()) {
                    it.addPredicate(DSL.or(usageTypeIn.map { usageSummary.matches(it) }))
                }
            }
            .page(
                first = first,
                after = after,
                mapQueryRecord = { record ->
                    DocumentGqlDto(
                        id = record[document.id]!!,
                        version = record[document.version]!!,
                        name = record[document.name]!!,
                        timeUploaded = record[document.timeUploaded]!!,
                        sizeInBytes = record[document.sizeInBytes],
                        storageId = record[document.storageId]!!,
                        mimeType = record[document.mimeType]!!,
                        usedBy = emptyList(),
                    )
                },
                postProcess = { records ->
                    val usagesByDocId = documentsRepository.findUsagesByDocumentIds(records.map { it.id })
                    records.map { item -> item.copy(usedBy = usagesByDocId[item.id] ?: emptyList()) }
                },
            )
    }

    private inner class DocumentUsageSummary(freeSearchText: String?) {
        private val tableName = name("document_usage_summary")
        private val documentUsagesTableName = name("document_usages")

        val documentId = field(tableName.append("document_id"), String::class.java)
        private val expenseUsages = field(tableName.append("expense_usages"), Int::class.java)
        private val incomeUsages = field(tableName.append("income_usages"), Int::class.java)
        private val invoiceUsages = field(tableName.append("invoice_usages"), Int::class.java)
        private val incomeTaxPaymentUsages = field(tableName.append("income_tax_payment_usages"), Int::class.java)
        private val standaloneDocumentUsages = field(tableName.append("standalone_document_usages"), Int::class.java)
        val matchingTitleUsages = field(tableName.append("matching_title_usages"), Int::class.java)

        private val usageDocumentId = field(documentUsagesTableName.append("document_id"), String::class.java)
        private val usageType = field(documentUsagesTableName.append("usage_type"), String::class.java)
        private val usageTitle = field(documentUsagesTableName.append("usage_title"), String::class.java)

        val table: Table<*> = buildTable(freeSearchText)

        fun matches(type: DocumentUsageFilterType) = when (type) {
            DocumentUsageFilterType.EXPENSE -> expenseUsages.greaterThan(0)
            DocumentUsageFilterType.INCOME -> incomeUsages.greaterThan(0)
            DocumentUsageFilterType.INVOICE -> invoiceUsages.greaterThan(0)
            DocumentUsageFilterType.INCOME_TAX_PAYMENT -> incomeTaxPaymentUsages.greaterThan(0)
            DocumentUsageFilterType.STANDALONE_DOCUMENT -> standaloneDocumentUsages.greaterThan(0)
            DocumentUsageFilterType.UNUSED -> documentId.isNull
        }

        private fun buildTable(freeSearchText: String?): Table<*> {
            val documentUsages = buildDocumentUsagesTable()
            val matchingTitleUsagesField = if (freeSearchText == null) {
                inline(0).`as`(matchingTitleUsages)
            } else {
                count().filterWhere(usageTitle.containsIgnoreCase(freeSearchText)).`as`(matchingTitleUsages)
            }

            return select(
                usageDocumentId.`as`(documentId),
                count().filterWhere(usageType.eq(DocumentUsageType.EXPENSE.name)).`as`(expenseUsages),
                count().filterWhere(usageType.eq(DocumentUsageType.INCOME.name)).`as`(incomeUsages),
                count().filterWhere(usageType.eq(DocumentUsageType.INVOICE.name)).`as`(invoiceUsages),
                count().filterWhere(usageType.eq(DocumentUsageType.INCOME_TAX_PAYMENT.name)).`as`(incomeTaxPaymentUsages),
                count().filterWhere(usageType.eq(DocumentUsageType.STANDALONE_DOCUMENT.name))
                    .`as`(standaloneDocumentUsages),
                matchingTitleUsagesField,
            )
                .from(documentUsages)
                .groupBy(usageDocumentId)
                .asTable(tableName.last())
        }

        private fun buildDocumentUsagesTable(): Table<*> = select(
            expenseAttachments.documentId.`as`(usageDocumentId),
            inline(DocumentUsageType.EXPENSE.name).`as`(usageType),
            expense.title.`as`(usageTitle),
        )
            .from(expenseAttachments)
            .join(expense).on(expense.id.eq(expenseAttachments.expenseId))
            .unionAll(
                select(
                    incomeAttachments.documentId.`as`(usageDocumentId),
                    inline(DocumentUsageType.INCOME.name).`as`(usageType),
                    income.title.`as`(usageTitle),
                )
                    .from(incomeAttachments)
                    .join(income).on(income.id.eq(incomeAttachments.incomeId))
            )
            .unionAll(
                select(
                    invoiceAttachments.documentId.`as`(usageDocumentId),
                    inline(DocumentUsageType.INVOICE.name).`as`(usageType),
                    invoice.title.`as`(usageTitle),
                )
                    .from(invoiceAttachments)
                    .join(invoice).on(invoice.id.eq(invoiceAttachments.invoiceId))
            )
            .unionAll(
                select(
                    incomeTaxPaymentAttachments.documentId.`as`(usageDocumentId),
                    inline(DocumentUsageType.INCOME_TAX_PAYMENT.name).`as`(usageType),
                    incomeTaxPayment.title.`as`(usageTitle),
                )
                    .from(incomeTaxPaymentAttachments)
                    .join(incomeTaxPayment).on(incomeTaxPayment.id.eq(incomeTaxPaymentAttachments.incomeTaxPaymentId))
            )
            .unionAll(
                select(
                    standaloneDocument.documentId.`as`(usageDocumentId),
                    inline(DocumentUsageType.STANDALONE_DOCUMENT.name).`as`(usageType),
                    standaloneDocument.title.`as`(usageTitle),
                )
                    .from(standaloneDocument)
            )
            .asTable(documentUsagesTableName.last())
    }
}
