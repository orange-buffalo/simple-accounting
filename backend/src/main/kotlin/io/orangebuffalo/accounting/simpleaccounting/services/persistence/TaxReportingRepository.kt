package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.LocalDate

@Component
class TaxReportingRepository(private val jdbcTemplate: JdbcTemplate) {

    fun getTaxReport(fromDate: LocalDate, toDate: LocalDate, workspace: Workspace): TaxReport {
        val taxReportItems = executeTaxReportQuery(fromDate, toDate, workspace)
        return fillTaxReport(taxReportItems)
    }

    private fun fillTaxReport(taxReportItems: List<TaxReportRawItem>): TaxReport {
        val finalizedCollectedTaxes = mutableListOf<FinalizedTaxSummaryItem>()
        val finalizedPaidTaxes = mutableListOf<FinalizedTaxSummaryItem>()
        val pendingCollectedTaxes = mutableListOf<PendingTaxSummaryItem>()
        val pendingPaidTaxes = mutableListOf<PendingTaxSummaryItem>()

        taxReportItems.forEach { reportItem ->
            if (reportItem.finalized!!) {
                val targetList = if (reportItem.paid!!) finalizedPaidTaxes else finalizedCollectedTaxes
                targetList.add(
                    FinalizedTaxSummaryItem(
                        tax = reportItem.taxId!!,
                        taxAmount = reportItem.taxAmount!!,
                        includedItemsAmount = reportItem.itemsAmount!!,
                        includedItemsNumber = reportItem.itemsCount!!
                    )
                )
            } else {
                val targetList = if (reportItem.paid!!) pendingPaidTaxes else pendingCollectedTaxes
                targetList.add(
                    PendingTaxSummaryItem(
                        tax = reportItem.taxId!!,
                        includedItemsNumber = reportItem.itemsCount!!
                    )
                )
            }
        }

        return TaxReport(
            finalizedCollectedTaxes = finalizedCollectedTaxes,
            finalizedPaidTaxes = finalizedPaidTaxes,
            pendingCollectedTaxes = pendingCollectedTaxes,
            pendingPaidTaxes = pendingPaidTaxes
        )
    }

    private fun executeTaxReportQuery(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<TaxReportRawItem> = jdbcTemplate.query(
        """
            select tax_data.tax_id,
                   sum(tax_data.tax_amount) as tax_amount,
                   count(tax_data.tax_id) as items_count,
                   sum(tax_data.amount) as items_amount,
                   tax_data.finalized,
                   tax_data.paid
            from (
            select e.tax_amount,
                   e.tax_id,
                   e.reported_amount_in_default_currency as amount,
                   e.reported_amount_in_default_currency > 0 as finalized,
                   true as paid,
                   e.date_paid as target_date,
                   e.workspace_id
            from expense e
            where e.tax_id is not null
            union all
            select i.tax_amount,
                   i.tax_id,
                   i.reported_amount_in_default_currency as amount,
                   i.reported_amount_in_default_currency > 0 as finalized,
                   false as paid,
                   i.date_received as target_date,
                   i.workspace_id
            from income i
            where i.tax_id is not null) tax_data
            where
                tax_data.target_date >= ?
                and tax_data.target_date <= ?
                and tax_data.workspace_id = ?
            group by tax_data.tax_id, finalized, paid
        """.trimIndent(),
        PreparedStatementSetter { ps ->
            ps.setDate(1, Date.valueOf(fromDate))
            ps.setDate(2, Date.valueOf(toDate))
            ps.setLong(3, workspace.id!!)
        },
        BeanPropertyRowMapper(TaxReportRawItem::class.java)
    )
}

class TaxReportRawItem {
    var taxId: Long? = null
    var taxAmount: Long? = null
    var itemsCount: Long? = null
    var itemsAmount: Long? = null
    var finalized: Boolean? = null
    var paid: Boolean? = null
}

data class TaxReport(
    var finalizedCollectedTaxes: List<FinalizedTaxSummaryItem>,
    var finalizedPaidTaxes: List<FinalizedTaxSummaryItem>,
    var pendingCollectedTaxes: List<PendingTaxSummaryItem>,
    var pendingPaidTaxes: List<PendingTaxSummaryItem>
)

data class FinalizedTaxSummaryItem(
    var tax: Long,
    var taxAmount: Long,
    var includedItemsNumber: Long,
    var includedItemsAmount: Long
)

data class PendingTaxSummaryItem(
    var tax: Long,
    var includedItemsNumber: Long
)