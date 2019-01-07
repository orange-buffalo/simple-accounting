package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.business.IncomeService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TaxPaymentService
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/statistics/")
class StatisticsApiController(
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService,
    private val incomeService: IncomeService,
    private val taxPaymentService: TaxPaymentService
) {

    @GetMapping("expenses")
    fun getExpensesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): Mono<ExpensesStatisticsDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val expensesStatistics = expenseService.getExpensesStatistics(fromDate, toDate, workspace)
        ExpensesStatisticsDto(
            expensesStatistics.map {
                ExpensesStatisticsItemDto(
                    it.categoryId,
                    it.totalAmount,
                    it.finalizedCount,
                    it.pendingCount
                )
            }
        )
    }

    @GetMapping("incomes")
    fun getIncomesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): Mono<IncomesStatisticsDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val incomesStatistics = incomeService.getIncomesStatistics(fromDate, toDate, workspace)
        IncomesStatisticsDto(
            incomesStatistics.map {
                IncomesStatisticsItemDto(
                    it.categoryId,
                    it.totalAmount,
                    it.finalizedCount,
                    it.pendingCount,
                    it.currencyExchangeGain
                )
            }
        )
    }

    @GetMapping("tax-payments")
    fun getTaxPaymentsStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): Mono<TaxPaymentsStatisticsDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val taxPaymentsStatistics = taxPaymentService.getTaxPaymentStatistics(fromDate, toDate, workspace)
        TaxPaymentsStatisticsDto(taxPaymentsStatistics.totalTaxPayments)
    }
}

data class TaxPaymentsStatisticsDto(
    val totalTaxPayments: Long
)

@Suppress("unused")
data class ExpensesStatisticsDto(
    val items: List<ExpensesStatisticsItemDto>
) {
    val totalAmount: Long = items.map { it.totalAmount }.sum()
    val finalizedCount: Long = items.map { it.finalizedCount }.sum()
    val pendingCount: Long = items.map { it.pendingCount }.sum()
}

data class ExpensesStatisticsItemDto(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long
)

@Suppress("unused")
data class IncomesStatisticsDto(
    val items: List<IncomesStatisticsItemDto>
) {
    val totalAmount: Long = items.map { it.totalAmount }.sum()
    val finalizedCount: Long = items.map { it.finalizedCount }.sum()
    val pendingCount: Long = items.map { it.pendingCount }.sum()
    val currencyExchangeGain = items.map { it.currencyExchangeGain }.sum()
}

data class IncomesStatisticsItemDto(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,
    val currencyExchangeGain: Long
)