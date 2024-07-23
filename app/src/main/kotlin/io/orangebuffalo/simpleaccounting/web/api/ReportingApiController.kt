package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.domain.generaltaxes.GeneralTaxReportingService
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.domain.generaltaxes.FinalizedGeneralTaxSummaryItem
import io.orangebuffalo.simpleaccounting.domain.generaltaxes.PendingGeneralTaxSummaryItem
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/reporting/")
class ReportingApiController(
    private val taxReportingService: GeneralTaxReportingService,
    private val workspaceService: WorkspaceService
) {

    @GetMapping("general-taxes")
    suspend fun getGeneralTaxReport(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): GeneralTaxReportDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val report = taxReportingService.getGeneralTaxReport(fromDate, toDate, workspace)
        return GeneralTaxReportDto(
            finalizedCollectedTaxes = report.finalizedCollectedTaxes.map(::convertFinalizedTaxItem),
            finalizedPaidTaxes = report.finalizedPaidTaxes.map(::convertFinalizedTaxItem),
            pendingPaidTaxes = report.pendingPaidTaxes.map(::convertPendingTaxItem),
            pendingCollectedTaxes = report.pendingCollectedTaxes.map(::convertPendingTaxItem)
        )
    }

    private fun convertPendingTaxItem(item: PendingGeneralTaxSummaryItem) = PendingTaxSummaryItemDto(
        tax = item.tax,
        includedItemsNumber = item.includedItemsNumber
    )

    private fun convertFinalizedTaxItem(item: FinalizedGeneralTaxSummaryItem) = FinalizedTaxSummaryItemDto(
        taxAmount = item.taxAmount,
        tax = item.tax,
        includedItemsNumber = item.includedItemsNumber,
        includedItemsAmount = item.includedItemsAmount
    )
}

data class GeneralTaxReportDto(
    var finalizedCollectedTaxes: List<FinalizedTaxSummaryItemDto>,
    var finalizedPaidTaxes: List<FinalizedTaxSummaryItemDto>,
    var pendingCollectedTaxes: List<PendingTaxSummaryItemDto>,
    var pendingPaidTaxes: List<PendingTaxSummaryItemDto>
)

data class FinalizedTaxSummaryItemDto(
    var tax: Long,
    var taxAmount: Long,
    var includedItemsNumber: Long,
    var includedItemsAmount: Long
)

data class PendingTaxSummaryItemDto(
    var tax: Long,
    var includedItemsNumber: Long
)
