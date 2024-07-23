package io.orangebuffalo.simpleaccounting.domain.expenses

import io.orangebuffalo.simpleaccounting.domain.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import java.time.LocalDate

interface ExpenseRepository : AbstractEntityRepository<Expense>, ExpenseRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Expense?
}

interface ExpenseRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<ExpensesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>
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
