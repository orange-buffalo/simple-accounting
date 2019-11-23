package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import com.querydsl.core.annotations.QueryProjection
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import java.time.LocalDate

interface ExpenseRepository :
    AbstractEntityRepository<Expense>, QuerydslPredicateExecutor<Expense>, ExpenseRepositoryExt {

    fun findByIdAndWorkspace(id: Long, workspace: Workspace): Expense?
}

interface ExpenseRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>
}

data class ExpensesStatistics @QueryProjection constructor(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,

    /**
     * The difference between converted amount and income taxable amount over all expenses of this category
     */
    val currencyExchangeDifference: Long
)
