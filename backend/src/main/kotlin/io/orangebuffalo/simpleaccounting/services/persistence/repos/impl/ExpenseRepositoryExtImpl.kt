package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.services.persistence.repos.ExpenseRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.repos.ExpensesStatistics
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ExpenseRepositoryExtImpl(
    private val dslContext: DSLContext
) : ExpenseRepositoryExt {

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

    override fun findByIdAndWorkspace(id: Long, workspace: Workspace): Expense? = dslContext
        .select()
        .from(expense)
        .where(
            expense.id.eq(id),
            expense.workspaceId.eq(workspace.id)
        )
        .fetchOneOrNull()

    override fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics> {
        val incomeTaxableAmount = expense.incomeTaxableAdjustedAmountInDefaultCurrency
        return dslContext
            .select(
                expense.categoryId.mapTo(ExpensesStatistics::categoryId),
                sum(
                    case_()
                        .`when`(incomeTaxableAmount.isNull,0L)
                        .otherwise(incomeTaxableAmount)
                ) .mapTo(ExpensesStatistics::totalAmount),
                count(
                    case_()
                        .`when`(expense.status.eq(ExpenseStatus.FINALIZED), 1L)
                        .otherwise(inline<Long>(null))
                ) .mapTo(ExpensesStatistics::finalizedCount),
                 count(
                    case_()
                        .`when`(expense.status.ne(ExpenseStatus.FINALIZED), 1L)
                        .otherwise(inline<Long>(null))
                ) .mapTo(ExpensesStatistics::pendingCount),
                sum(
                    case_()
                        .`when`(expense.status.ne(ExpenseStatus.FINALIZED),0L)
                        .otherwise(expense.convertedAdjustedAmountInDefaultCurrency
                                .subtract(expense.incomeTaxableAdjustedAmountInDefaultCurrency))
                ) .mapTo(ExpensesStatistics::currencyExchangeDifference)
            )
            .from(expense)
            .where(
                expense.workspaceId.eq(workspace.id),
                expense.datePaid.greaterOrEqual(fromDate),
                expense.datePaid.lessOrEqual(toDate)
            )
            .groupBy(expense.categoryId)
            .fetchListOf()
    }
}
