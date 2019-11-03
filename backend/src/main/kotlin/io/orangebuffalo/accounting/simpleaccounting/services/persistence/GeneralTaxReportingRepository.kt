package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.LocalDate

@Component
class GeneralTaxReportingRepository(private val jdbcTemplate: JdbcTemplate) {

    fun getGeneralTaxReport(fromDate: LocalDate, toDate: LocalDate, workspace: Workspace): GeneralTaxReport {
        val taxReportItems = executeTaxReportQuery(fromDate, toDate, workspace)
        return fillTaxReport(taxReportItems)
    }

    private fun fillTaxReport(taxReportItems: List<GeneralTaxReportRawItem>): GeneralTaxReport {
        val finalizedCollectedTaxes = mutableListOf<FinalizedGeneralTaxSummaryItem>()
        val finalizedPaidTaxes = mutableListOf<FinalizedGeneralTaxSummaryItem>()
        val pendingCollectedTaxes = mutableListOf<PendingGeneralTaxSummaryItem>()
        val pendingPaidTaxes = mutableListOf<PendingGeneralTaxSummaryItem>()

        taxReportItems.forEach { reportItem ->
            if (reportItem.finalized!!) {
                val targetList = if (reportItem.paid!!) finalizedPaidTaxes else finalizedCollectedTaxes
                targetList.add(
                    FinalizedGeneralTaxSummaryItem(
                        tax = reportItem.taxId!!,
                        taxAmount = reportItem.taxAmount!!,
                        includedItemsAmount = reportItem.itemsAmount!!,
                        includedItemsNumber = reportItem.itemsCount!!
                    )
                )
            } else {
                val targetList = if (reportItem.paid!!) pendingPaidTaxes else pendingCollectedTaxes
                targetList.add(
                    PendingGeneralTaxSummaryItem(
                        tax = reportItem.taxId!!,
                        includedItemsNumber = reportItem.itemsCount!!
                    )
                )
            }
        }

        return GeneralTaxReport(
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
    ): List<GeneralTaxReportRawItem> = jdbcTemplate.query(
        """
            select tax_data.tax_id,
                   sum(tax_data.tax_amount) as tax_amount,
                   count(tax_data.tax_id) as items_count,
                   sum(tax_data.amount) as items_amount,
                   tax_data.finalized,
                   tax_data.paid
            from (
            select e.general_tax_amount as tax_amount,
                   e.general_tax_id as tax_id,
                   e.reported_amount_in_default_currency as amount,
                   e.reported_amount_in_default_currency > 0 as finalized,
                   true as paid,
                   e.date_paid as target_date,
                   e.workspace_id
            from expense e
            where e.general_tax_id is not null
            union all
            select i.general_tax_amount as tax_amount,
                   i.general_tax_id as tax_id,
                   i.reported_amount_in_default_currency as amount,
                   i.reported_amount_in_default_currency > 0 as finalized,
                   false as paid,
                   i.date_received as target_date,
                   i.workspace_id
            from income i
            where i.general_tax_id is not null) tax_data
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
        BeanPropertyRowMapper(GeneralTaxReportRawItem::class.java)
    )
}

class GeneralTaxReportRawItem {
    var taxId: Long? = null
    var taxAmount: Long? = null
    var itemsCount: Long? = null
    var itemsAmount: Long? = null
    var finalized: Boolean? = null
    var paid: Boolean? = null
}

data class GeneralTaxReport(
    var finalizedCollectedTaxes: List<FinalizedGeneralTaxSummaryItem>,
    var finalizedPaidTaxes: List<FinalizedGeneralTaxSummaryItem>,
    var pendingCollectedTaxes: List<PendingGeneralTaxSummaryItem>,
    var pendingPaidTaxes: List<PendingGeneralTaxSummaryItem>
)

data class FinalizedGeneralTaxSummaryItem(
    var tax: Long,
    var taxAmount: Long,
    var includedItemsNumber: Long,
    var includedItemsAmount: Long
)

data class PendingGeneralTaxSummaryItem(
    var tax: Long,
    var includedItemsNumber: Long
)
