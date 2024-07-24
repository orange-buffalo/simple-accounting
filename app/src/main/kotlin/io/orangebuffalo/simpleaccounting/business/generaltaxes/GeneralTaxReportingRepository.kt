package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus
import io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.infra.jooq.fieldOrFail
import io.orangebuffalo.simpleaccounting.infra.jooq.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class GeneralTaxReportingRepository(private val dslContext: DSLContext) {

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

    private object TaxReportFields {
        const val taxAmount = "tax_amount"
        const val taxId = "tax_id"
        const val itemsAmount = "items_amount"
        const val finalized = "finalized"
        const val paid = "paid"
        const val targetDateField = "target_date"
        const val workspaceIdField = "workspace_id"
    }

    private fun executeTaxReportQuery(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<GeneralTaxReportRawItem> {
        val expense = Tables.EXPENSE
        val income = Tables.INCOME

        val taxData = dslContext
            .select(
                expense.generalTaxAmount.`as`(TaxReportFields.taxAmount),
                expense.generalTaxId.`as`(TaxReportFields.taxId),
                coalesce(expense.incomeTaxableAdjustedAmountInDefaultCurrency, 0).`as`(TaxReportFields.itemsAmount),
                field(expense.status.eq(ExpenseStatus.FINALIZED)).`as`(TaxReportFields.finalized),
                inline(true).`as`(TaxReportFields.paid),
                expense.datePaid.`as`(TaxReportFields.targetDateField),
                expense.workspaceId.`as`(TaxReportFields.workspaceIdField)
            )
            .from(expense)
            .where(expense.generalTaxId.isNotNull)
            .unionAll(
                select(
                    income.generalTaxAmount,
                    income.generalTaxId,
                    coalesce(income.incomeTaxableAdjustedAmountInDefaultCurrency, 0),
                    field(income.status.eq(IncomeStatus.FINALIZED)),
                    inline(false),
                    income.dateReceived,
                    income.workspaceId
                )
                    .from(income)
                    .where(income.generalTaxId.isNotNull)
            )

        return dslContext
            .select(
                taxData.fieldOrFail<Long>(TaxReportFields.taxId)
                    .mapTo(GeneralTaxReportRawItem::taxId),
                sum(taxData.field(TaxReportFields.taxAmount, Long::class.java))
                    .mapTo(GeneralTaxReportRawItem::taxAmount),
                count(taxData.field(TaxReportFields.taxId))
                    .mapTo(GeneralTaxReportRawItem::itemsCount),
                sum(taxData.field(TaxReportFields.itemsAmount, Long::class.java))
                    .mapTo(GeneralTaxReportRawItem::itemsAmount),
                taxData.fieldOrFail<Boolean>(TaxReportFields.finalized)
                    .mapTo(GeneralTaxReportRawItem::finalized),
                taxData.fieldOrFail<Boolean>(TaxReportFields.paid)
                    .mapTo(GeneralTaxReportRawItem::paid)
            )
            .from(taxData)
            .where(
                taxData.fieldOrFail<LocalDate>(TaxReportFields.targetDateField).greaterOrEqual(fromDate),
                taxData.fieldOrFail<LocalDate>(TaxReportFields.targetDateField).lessOrEqual(toDate),
                taxData.fieldOrFail<Long>(TaxReportFields.workspaceIdField).eq(workspace.id)
            )
            .groupBy(
                taxData.field(TaxReportFields.taxId),
                taxData.field(TaxReportFields.finalized),
                taxData.field(TaxReportFields.paid)
            )
            .fetchListOf()
    }
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
