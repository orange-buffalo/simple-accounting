package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/statistics/")
class StatisticsApiController(
    private val expenseService: ExpenseService,
    private val incomeService: IncomeService,
    private val incomeTaxPaymentService: IncomeTaxPaymentService,
    private val workspaceService: WorkspaceService
) {

    @GetMapping("expenses")
    suspend fun getExpensesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): IncomesExpensesStatisticsDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expensesStatistics = expenseService.getExpensesStatistics(fromDate, toDate, workspace)
        return IncomesExpensesStatisticsDto(
            expensesStatistics.map {
                IncomeExpensesStatisticsItemDto(
                    categoryId = it.categoryId,
                    totalAmount = it.totalAmount,
                    finalizedCount = it.finalizedCount,
                    pendingCount = it.pendingCount,
                    currencyExchangeDifference = it.currencyExchangeDifference
                )
            }
        )
    }

    @GetMapping("incomes")
    suspend fun getIncomesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): IncomesExpensesStatisticsDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val incomesStatistics = incomeService.getIncomesStatistics(fromDate, toDate, workspace)
        return IncomesExpensesStatisticsDto(
            incomesStatistics.map {
                IncomeExpensesStatisticsItemDto(
                    categoryId = it.categoryId,
                    totalAmount = it.totalAmount,
                    finalizedCount = it.finalizedCount,
                    pendingCount = it.pendingCount,
                    currencyExchangeDifference = it.currencyExchangeDifference
                )
            }
        )
    }

    @GetMapping("income-tax-payments")
    suspend fun getTaxPaymentsStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): IncomeTaxPaymentsStatisticsDto {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val incomeTaxPaymentsStatistics = incomeTaxPaymentService.getTaxPaymentStatistics(fromDate, toDate, workspaceId)
        return IncomeTaxPaymentsStatisticsDto(
            incomeTaxPaymentsStatistics.totalTaxPayments
        )
    }

    @GetMapping("currencies-shortlist")
    suspend fun getCurrenciesShortlist(@PathVariable workspaceId: Long): List<String> {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expensesCurrencies = expenseService.getCurrenciesUsageStatistics(workspace)
        val incomesCurrencies = incomeService.getCurrenciesUsageStatistics(workspace)
        return (expensesCurrencies + incomesCurrencies).asSequence()
            .groupingBy(CurrenciesUsageStatistics::currency)
            .reduce { currency, accumulator, next ->
                CurrenciesUsageStatistics(
                    currency = currency,
                    count = accumulator.count + next.count
                )
            }
            .values.asSequence()
            .sortedWith(
                Comparator.comparing(CurrenciesUsageStatistics::count).reversed()
                    .thenComparing(CurrenciesUsageStatistics::currency)
            )
            .map(CurrenciesUsageStatistics::currency)
            .toList()
    }
}

data class IncomeTaxPaymentsStatisticsDto(
    val totalTaxPayments: Long
)

@Suppress("unused")
data class IncomesExpensesStatisticsDto(
    val items: List<IncomeExpensesStatisticsItemDto>
) {
    val totalAmount: Long = items.map { it.totalAmount }.sum()
    val finalizedCount: Long = items.map { it.finalizedCount }.sum()
    val pendingCount: Long = items.map { it.pendingCount }.sum()
    val currencyExchangeDifference: Long = items.map { it.currencyExchangeDifference }.sum()
}

data class IncomeExpensesStatisticsItemDto(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,
    val currencyExchangeDifference: Long
)
