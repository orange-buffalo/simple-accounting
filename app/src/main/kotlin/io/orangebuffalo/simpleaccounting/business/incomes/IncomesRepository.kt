package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import java.time.LocalDate

interface IncomesRepository : AbstractEntityRepository<Income>, IncomesRepositoryExt {
    fun findByIdAndWorkspaceId(incomeId: String, workspaceId: String): Income?
}

interface IncomesRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: String
    ): List<IncomesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>
}

data class IncomesStatistics(
    val categoryId: String?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,

    /**
     * The difference between converted amount and income taxable amount over all incomes of this category
     */
    val currencyExchangeDifference: Long
)
