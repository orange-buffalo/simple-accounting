package io.orangebuffalo.simpleaccounting.business.analytics

import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import org.springframework.stereotype.Service

@Service
class WorkspaceAnalyticsService(
    private val expenseService: ExpenseService,
    private val incomesService: IncomesService,
) {
    suspend fun getCurrenciesShortlist(workspace: Workspace): List<String> {
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
