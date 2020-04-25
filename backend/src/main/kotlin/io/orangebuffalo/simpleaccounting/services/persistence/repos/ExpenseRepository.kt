package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import java.time.LocalDate

interface ExpenseRepository : AbstractEntityRepository<Expense>, ExpenseRepositoryExt

interface ExpenseRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>

    fun findByIdAndWorkspace(id: Long, workspace: Workspace): Expense?
}

data class ExpensesStatistics(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,

    /**
     * The difference between converted amount and income taxable amount over all expenses of this category
     */
    val currencyExchangeDifference: Long
)
