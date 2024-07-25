package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import java.time.LocalDate

interface IncomesRepository : AbstractEntityRepository<Income>, IncomesRepositoryExt {
    fun findByIdAndWorkspaceId(incomeId: Long, workspaceId: Long): Income?
}

interface IncomesRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<IncomesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>
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
