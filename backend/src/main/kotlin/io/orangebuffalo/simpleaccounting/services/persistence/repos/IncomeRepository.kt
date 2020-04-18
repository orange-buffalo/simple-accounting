package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import java.time.LocalDate

interface IncomeRepository : AbstractEntityRepository<Income>, IncomeRepositoryExt

interface IncomeRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<IncomesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>

    fun findByIdAndWorkspaceId(incomeId: Long, workspaceId: Long): Income?
}

data class IncomesStatistics(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,

    /**
     * The difference between converted amount and income taxable amount over all incomes of this category
     */
    val currencyExchangeDifference: Long
)
