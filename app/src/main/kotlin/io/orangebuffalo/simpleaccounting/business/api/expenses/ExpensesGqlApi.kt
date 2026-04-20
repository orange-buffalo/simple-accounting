package io.orangebuffalo.simpleaccounting.business.api.expenses

import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

@Component
class ExpensesGqlApi(
    private val paginationService: GraphqlPaginationService,
    private val dslContext: DSLContext,
) {
    private val expense = Tables.EXPENSE
    private val category = Tables.CATEGORY
    private val expenseAttachments = Tables.EXPENSE_ATTACHMENTS

    suspend fun loadExpenses(
        workspaceId: Long,
        first: Int,
        after: String?,
        freeSearchText: String?,
    ): ConnectionGqlDto<ExpenseGqlDto> {
        return paginationService.forTable(expense)
            .onQuery { it.leftJoin(category).on(category.id.eq(expense.categoryId)) }
            .addPredicate(expense.workspaceId.eq(workspaceId))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(
                        DSL.or(
                            expense.notes.containsIgnoreCase(freeSearchText),
                            expense.title.containsIgnoreCase(freeSearchText),
                            category.name.containsIgnoreCase(freeSearchText),
                        )
                    )
                }
            }
            .page(
                first = first,
                after = after,
                sortFields = listOf(expense.datePaid.desc(), expense.createdAt.asc()),
                mapQueryRecord = { record ->
                    ExpenseGqlDto(
                        id = record[expense.id]!!,
                        version = record[expense.version]!!,
                        title = record[expense.title]!!,
                        datePaid = record[expense.datePaid]!!,
                        currency = record[expense.currency]!!,
                        originalAmount = record[expense.originalAmount]!!,
                        convertedAmounts = ExpenseAmountsGqlDto(
                            originalAmountInDefaultCurrency = record[expense.convertedOriginalAmountInDefaultCurrency],
                            adjustedAmountInDefaultCurrency = record[expense.convertedAdjustedAmountInDefaultCurrency],
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = record[expense.useDifferentExchangeRateForIncomeTaxPurposes]!!,
                        incomeTaxableAmounts = ExpenseAmountsGqlDto(
                            originalAmountInDefaultCurrency = record[expense.incomeTaxableOriginalAmountInDefaultCurrency],
                            adjustedAmountInDefaultCurrency = record[expense.incomeTaxableAdjustedAmountInDefaultCurrency],
                        ),
                        percentOnBusiness = record[expense.percentOnBusiness]!!,
                        notes = record[expense.notes],
                        createdAt = record[expense.createdAt]!!,
                        status = record[expense.status]!!,
                        generalTaxId = record[expense.generalTaxId],
                        generalTaxRateInBps = record[expense.generalTaxRateInBps],
                        generalTaxAmount = record[expense.generalTaxAmount],
                        categoryId = record[expense.categoryId],
                        attachmentIds = emptyList(),
                        workspaceId = workspaceId,
                    )
                },
                postProcess = { records ->
                    val attachmentsByExpenseId = dslContext
                        .select(expenseAttachments.expenseId, expenseAttachments.documentId)
                        .from(expenseAttachments)
                        .where(expenseAttachments.expenseId.`in`(records.map { it.id }))
                        .fetch()
                        .groupBy(
                            { it[expenseAttachments.expenseId]!! },
                            { it[expenseAttachments.documentId]!! },
                        )
                    records.map { dto ->
                        dto.copy(attachmentIds = attachmentsByExpenseId[dto.id] ?: emptyList())
                    }
                },
            )
    }
}
