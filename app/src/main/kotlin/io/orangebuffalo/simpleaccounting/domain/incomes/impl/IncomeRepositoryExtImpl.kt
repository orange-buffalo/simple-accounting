package io.orangebuffalo.simpleaccounting.domain.incomes.impl

import io.orangebuffalo.simpleaccounting.domain.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.domain.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.infra.jooq.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.domain.incomes.IncomeRepositoryExt
import io.orangebuffalo.simpleaccounting.domain.incomes.IncomesStatistics
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class IncomeRepositoryExtImpl(
    private val dslContext: DSLContext
) : IncomeRepositoryExt {

    private val income = Tables.INCOME

    override fun getCurrenciesUsageStatistics(
        workspace: Workspace
    ): List<CurrenciesUsageStatistics> = dslContext
        .select(income.currency, count(income.id))
        .from(income)
        .groupBy(income.currency)
        .fetchListOf()

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<IncomesStatistics> {
        val incomeTaxableAmount = income.incomeTaxableAdjustedAmountInDefaultCurrency
        return dslContext
            .select(
                income.categoryId.mapTo(IncomesStatistics::categoryId),
                sum(
                    case_()
                        .`when`(incomeTaxableAmount.isNull, 0L)
                        .otherwise(incomeTaxableAmount)
                ).mapTo(IncomesStatistics::totalAmount),
                count(
                    case_()
                        .`when`(income.status.eq(IncomeStatus.FINALIZED), 1L)
                        .otherwise(inline<Long>(null))
                ).mapTo(IncomesStatistics::finalizedCount),
                count(
                    case_()
                        .`when`(income.status.ne(IncomeStatus.FINALIZED), 1L)
                        .otherwise(inline<Long>(null))
                ).mapTo(IncomesStatistics::pendingCount),
                sum(
                    case_()
                        .`when`(income.status.ne(IncomeStatus.FINALIZED), 0L)
                        .otherwise(
                            income.convertedAdjustedAmountInDefaultCurrency
                                .sub(income.incomeTaxableAdjustedAmountInDefaultCurrency)
                        )
                ).mapTo(IncomesStatistics::currencyExchangeDifference)
            )
            .from(income)
            .where(
                income.workspaceId.eq(workspaceId),
                income.dateReceived.greaterOrEqual(fromDate),
                income.dateReceived.lessOrEqual(toDate)
            )
            .groupBy(income.categoryId)
            .fetchListOf()
    }
}
