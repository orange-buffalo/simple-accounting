package io.orangebuffalo.simpleaccounting.business.expenses

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import java.time.LocalDate

interface ExpensesRepository : AbstractEntityRepository<Expense>, ExpensesRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Expense?
    fun findAllByWorkspaceId(workspaceId: Long): List<Expense>
    fun findAllByWorkspaceIdIn(workspaceIds: Set<Long>): List<Expense>
}

interface ExpensesRepositoryExt {
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
