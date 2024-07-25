package io.orangebuffalo.simpleaccounting.business.expenses.impl

import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.infra.jooq.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.business.expenses.ExpensesRepositoryExt
import io.orangebuffalo.simpleaccounting.business.expenses.ExpensesStatistics
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ExpensesRepositoryExtImpl(
    private val dslContext: DSLContext
) : ExpensesRepositoryExt {

    private val expense = Tables.EXPENSE

    override fun getCurrenciesUsageStatistics(
        workspace: Workspace
    ): List<CurrenciesUsageStatistics> = dslContext
        .select(
            expense.currency.mapTo(CurrenciesUsageStatistics::currency),
            count(expense.id).mapTo(CurrenciesUsageStatistics::count)
        )
        .from(expense)
        .where(expense.workspaceId.eq(workspace.id))
        .groupBy(expense.currency)
        .fetchListOf()

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<ExpensesStatistics> {
        val incomeTaxableAmount = expense.incomeTaxableAdjustedAmountInDefaultCurrency
        return dslContext
            .select(
                expense.categoryId.mapTo(ExpensesStatistics::categoryId),
                sum(
                    case_()
                        .`when`(incomeTaxableAmount.isNull, 0L)
                        .otherwise(incomeTaxableAmount)
                ).mapTo(ExpensesStatistics::totalAmount),
                count(
                    case_()
                        .`when`(expense.status.eq(ExpenseStatus.FINALIZED), 1L)
                        .otherwise(inline<Long>(null))
                ).mapTo(ExpensesStatistics::finalizedCount),
                count(
                    case_()
                        .`when`(expense.status.ne(ExpenseStatus.FINALIZED), 1L)
                        .otherwise(inline<Long>(null))
                ).mapTo(ExpensesStatistics::pendingCount),
                sum(
                    case_()
                        .`when`(expense.status.ne(ExpenseStatus.FINALIZED), 0L)
                        .otherwise(
                            expense.convertedAdjustedAmountInDefaultCurrency
                                .subtract(expense.incomeTaxableAdjustedAmountInDefaultCurrency)
                        )
                ).mapTo(ExpensesStatistics::currencyExchangeDifference)
            )
            .from(expense)
            .where(
                expense.workspaceId.eq(workspaceId),
                expense.datePaid.greaterOrEqual(fromDate),
                expense.datePaid.lessOrEqual(toDate)
            )
            .groupBy(expense.categoryId)
            .fetchListOf()
    }
}
