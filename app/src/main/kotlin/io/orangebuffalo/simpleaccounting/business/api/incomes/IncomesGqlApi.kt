package io.orangebuffalo.simpleaccounting.business.api.incomes

import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

@Component
class IncomesGqlApi(
    private val paginationService: GraphqlPaginationService,
    private val dslContext: DSLContext,
    private val incomesService: IncomesService,
) {
    private val income = Tables.INCOME
    private val category = Tables.CATEGORY
    private val incomeAttachments = Tables.INCOME_ATTACHMENTS

    suspend fun loadIncomes(
        workspaceId: Long,
        first: Int,
        after: String?,
        freeSearchText: String?,
    ): ConnectionGqlDto<IncomeGqlDto> {
        return paginationService.forTable(income)
            .onQuery { it.leftJoin(category).on(category.id.eq(income.categoryId)) }
            .addPredicate(income.workspaceId.eq(workspaceId))
            .also {
                if (freeSearchText != null) {
                    it.addPredicate(
                        DSL.or(
                            income.notes.containsIgnoreCase(freeSearchText),
                            income.title.containsIgnoreCase(freeSearchText),
                            category.name.containsIgnoreCase(freeSearchText),
                        )
                    )
                }
            }
            .page(
                first = first,
                after = after,
                sortFields = listOf(income.dateReceived.desc(), income.createdAt.asc()),
                mapQueryRecord = { record ->
                    IncomeGqlDto(
                        id = record[income.id]!!,
                        version = record[income.version]!!,
                        title = record[income.title]!!,
                        dateReceived = record[income.dateReceived]!!,
                        currency = record[income.currency]!!,
                        originalAmount = record[income.originalAmount]!!,
                        convertedAmounts = IncomeAmountsGqlDto(
                            originalAmountInDefaultCurrency = record[income.convertedOriginalAmountInDefaultCurrency],
                            adjustedAmountInDefaultCurrency = record[income.convertedAdjustedAmountInDefaultCurrency],
                        ),
                        useDifferentExchangeRateForIncomeTaxPurposes = record[income.useDifferentExchangeRateForIncomeTaxPurposes]!!,
                        incomeTaxableAmounts = IncomeAmountsGqlDto(
                            originalAmountInDefaultCurrency = record[income.incomeTaxableOriginalAmountInDefaultCurrency],
                            adjustedAmountInDefaultCurrency = record[income.incomeTaxableAdjustedAmountInDefaultCurrency],
                        ),
                        notes = record[income.notes],
                        createdAt = record[income.createdAt]!!,
                        status = record[income.status]!!,
                        generalTaxRateInBps = record[income.generalTaxRateInBps],
                        generalTaxAmount = record[income.generalTaxAmount],
                        generalTaxId = record[income.generalTaxId],
                        categoryId = record[income.categoryId],
                        workspaceId = workspaceId,
                        attachmentIds = emptyList(),
                        linkedInvoiceId = record[income.linkedInvoiceId],
                    )
                },
                postProcess = { records ->
                    val attachmentsByIncomeId = dslContext
                        .select(incomeAttachments.incomeId, incomeAttachments.documentId)
                        .from(incomeAttachments)
                        .where(incomeAttachments.incomeId.`in`(records.map { it.id }))
                        .fetch()
                        .groupBy(
                            { it[incomeAttachments.incomeId]!! },
                            { it[incomeAttachments.documentId]!! },
                        )
                    records.map { dto ->
                        dto.copy(attachmentIds = attachmentsByIncomeId[dto.id] ?: emptyList())
                    }
                },
            )
    }

    suspend fun loadIncome(workspaceId: Long, incomeId: Long): IncomeGqlDto? {
        return incomesService.getIncomeByIdAndWorkspaceId(incomeId, workspaceId)
            ?.toIncomeGqlDto()
    }
}
