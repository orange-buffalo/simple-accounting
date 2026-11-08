package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import java.time.LocalDate
import org.springframework.data.jdbc.repository.query.Query

interface IncomesRepository : AbstractEntityRepository<Income>, IncomesRepositoryExt {
    @Query("""
        select i.* from income i
        where i.workspace_id = :workspaceId and i.converted_original_amount_in_default_currency is null
          and exists (select 1 from account_statement_record r
                      where r.linked_invoice_id = i.linked_invoice_id and r.integration_id = :integrationId)
    """)
    fun findForStatementConversion(workspaceId: String, integrationId: String): List<Income>

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
