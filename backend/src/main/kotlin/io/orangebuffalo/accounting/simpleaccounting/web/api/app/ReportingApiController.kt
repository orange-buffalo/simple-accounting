package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.TaxReportingService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.FinalizedTaxSummaryItem
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.PendingTaxSummaryItem
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDate

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/reporting/")
class ReportingApiController(
    private val taxReportingService: TaxReportingService,
    private val extensions: ApiControllersExtensions
) {

    @GetMapping("taxes")
    fun getTaxReport(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): Mono<TaxReportDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val report = taxReportingService.getTaxReport(fromDate, toDate, workspace)
        TaxReportDto(
            finalizedCollectedTaxes = report.finalizedCollectedTaxes.map(::convertFinalizedTaxItem),
            finalizedPaidTaxes = report.finalizedPaidTaxes.map(::convertFinalizedTaxItem),
            pendingPaidTaxes = report.pendingPaidTaxes.map(::convertPendingTaxItem),
            pendingCollectedTaxes = report.pendingCollectedTaxes.map(::convertPendingTaxItem)
        )
    }

    private fun convertPendingTaxItem(item: PendingTaxSummaryItem) = PendingTaxSummaryItemDto(
        tax = item.tax,
        includedItemsNumber = item.includedItemsNumber
    )

    private fun convertFinalizedTaxItem(item: FinalizedTaxSummaryItem) = FinalizedTaxSummaryItemDto(
        taxAmount = item.taxAmount,
        tax = item.tax,
        includedItemsNumber = item.includedItemsNumber,
        includedItemsAmount = item.includedItemsAmount
    )
}

data class TaxReportDto(
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