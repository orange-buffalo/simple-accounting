package io.orangebuffalo.simpleaccounting.business.api.documents

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.Condition
import org.jooq.impl.DSL
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.selectOne
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
    ): ConnectionGqlDto<DocumentGqlDto> = paginationService.forTable(document)
        .addPredicate(document.workspaceId.eq(workspaceId))
        .also {
            if (freeSearchText != null) {
                it.addPredicate(
                    DSL.or(
                        document.name.containsIgnoreCase(freeSearchText),
                        usageExists(DocumentUsageType.EXPENSE, expense.title.containsIgnoreCase(freeSearchText)),
                        usageExists(DocumentUsageType.INCOME, income.title.containsIgnoreCase(freeSearchText)),
                        usageExists(DocumentUsageType.INVOICE, invoice.title.containsIgnoreCase(freeSearchText)),
                        usageExists(
                            DocumentUsageType.INCOME_TAX_PAYMENT,
                            incomeTaxPayment.title.containsIgnoreCase(freeSearchText),
                        ),
                        usageExists(
                            DocumentUsageType.STANDALONE_DOCUMENT,
                            standaloneDocument.title.containsIgnoreCase(freeSearchText),
                        ),
                    )
                )
            }
            if (!storageIdsIn.isNullOrEmpty()) {
                it.addPredicate(document.storageId.`in`(storageIdsIn))
            }
            if (!usageTypeIn.isNullOrEmpty()) {
                it.addPredicate(DSL.or(usageTypeIn.map { usageFilterMatches(it) }))
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

    private fun usageFilterMatches(type: DocumentUsageFilterType): Condition =
        type.toDocumentUsageType()?.let { usageExists(it) } ?: noUsagesExist()

    private fun noUsagesExist(): Condition = DSL.not(DSL.or(DocumentUsageType.entries.map { usageExists(it) }))

    private fun usageExists(type: DocumentUsageType, usageTitleCondition: Condition? = null): Condition = when (type) {
        DocumentUsageType.EXPENSE -> exists(
            selectOne()
                .from(expenseAttachments)
                .join(expense).on(expense.id.eq(expenseAttachments.expenseId))
                .where(
                    expenseAttachments.documentId.eq(document.id),
                    usageTitleCondition ?: DSL.trueCondition(),
                )
        )

        DocumentUsageType.INCOME -> exists(
            selectOne()
                .from(incomeAttachments)
                .join(income).on(income.id.eq(incomeAttachments.incomeId))
                .where(
                    incomeAttachments.documentId.eq(document.id),
                    usageTitleCondition ?: DSL.trueCondition(),
                )
        )

        DocumentUsageType.INVOICE -> exists(
            selectOne()
                .from(invoiceAttachments)
                .join(invoice).on(invoice.id.eq(invoiceAttachments.invoiceId))
                .where(
                    invoiceAttachments.documentId.eq(document.id),
                    usageTitleCondition ?: DSL.trueCondition(),
                )
        )

        DocumentUsageType.INCOME_TAX_PAYMENT -> exists(
            selectOne()
                .from(incomeTaxPaymentAttachments)
                .join(incomeTaxPayment).on(incomeTaxPayment.id.eq(incomeTaxPaymentAttachments.incomeTaxPaymentId))
                .where(
                    incomeTaxPaymentAttachments.documentId.eq(document.id),
                    usageTitleCondition ?: DSL.trueCondition(),
                )
        )

        DocumentUsageType.STANDALONE_DOCUMENT -> exists(
            selectOne()
                .from(standaloneDocument)
                .where(
                    standaloneDocument.documentId.eq(document.id),
                    usageTitleCondition ?: DSL.trueCondition(),
                )
        )
    }
}
