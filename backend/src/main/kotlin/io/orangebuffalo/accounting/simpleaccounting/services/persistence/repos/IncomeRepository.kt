package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import com.querydsl.core.annotations.QueryProjection
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import java.time.LocalDate

interface IncomeRepository :
    AbstractEntityRepository<Income>, QuerydslPredicateExecutor<Income>, IncomeRepositoryExt {

    fun findByIdAndWorkspace(id: Long, workspace: Workspace): Income?
}

interface IncomeRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<IncomesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>
}

data class IncomesStatistics @QueryProjection constructor(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,
    val currencyExchangeGain: Long
)