package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/statistics/")
class StatisticsApiController(
    private val expenseService: ExpenseService,
    private val incomesService: IncomesService,
    private val incomeTaxPaymentService: IncomeTaxPaymentService,
    private val workspacesService: WorkspacesService
) {

    @GetMapping("expenses")
    suspend fun getExpensesStatistics(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): IncomesExpensesStatisticsDto {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expensesStatistics = expenseService.getExpensesStatistics(fromDate, toDate, workspaceId)
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
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val incomesStatistics = incomesService.getIncomesStatistics(fromDate, toDate, workspaceId)
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
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val incomeTaxPaymentsStatistics = incomeTaxPaymentService.getTaxPaymentStatistics(fromDate, toDate, workspaceId)
        return IncomeTaxPaymentsStatisticsDto(
            incomeTaxPaymentsStatistics.totalTaxPayments
        )
    }

    @GetMapping("currencies-shortlist")
    suspend fun getCurrenciesShortlist(@PathVariable workspaceId: Long): List<String> {
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val expensesCurrencies = expenseService.getCurrenciesUsageStatistics(workspace)
        val incomesCurrencies = incomesService.getCurrenciesUsageStatistics(workspace)
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
    val totalAmount: Long = items.sumOf { it.totalAmount }
    val finalizedCount: Long = items.sumOf { it.finalizedCount }
    val pendingCount: Long = items.sumOf { it.pendingCount }
    val currencyExchangeDifference: Long = items.sumOf { it.currencyExchangeDifference }
}

data class IncomeExpensesStatisticsItemDto(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,
    val currencyExchangeDifference: Long
)
