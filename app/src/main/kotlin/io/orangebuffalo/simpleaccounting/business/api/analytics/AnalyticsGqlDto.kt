package io.orangebuffalo.simpleaccounting.business.api.analytics

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import io.orangebuffalo.simpleaccounting.business.incomes.IncomesService
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@GraphQLName("WorkspaceAnalytics")
@GraphQLDescription("Analytics data for a workspace.")
class AnalyticsGqlDto(private val workspaceId: Long) {

    @GraphQLDescription("Summary of expenses in the given date range.")
    suspend fun expensesSummary(
        @GraphQLDescription("Start date of the range (inclusive).") fromDate: LocalDate,
        @GraphQLDescription("End date of the range (inclusive).") toDate: LocalDate,
        env: DataFetchingEnvironment,
    ): ExpensesSummaryGqlDto {
        val expenseService = env.graphQlContext.getBean<ExpenseService>()
        val statistics = expenseService.getExpensesStatistics(fromDate, toDate, workspaceId)
        return ExpensesSummaryGqlDto(
            items = statistics.map {
                ExpensesSummaryItemGqlDto(
                    categoryId = it.categoryId,
                    totalAmount = it.totalAmount,
                    finalizedCount = it.finalizedCount,
                    pendingCount = it.pendingCount,
                    currencyExchangeDifference = it.currencyExchangeDifference,
                )
            }
        )
    }

    @GraphQLDescription("Summary of incomes in the given date range.")
    suspend fun incomesSummary(
        @GraphQLDescription("Start date of the range (inclusive).") fromDate: LocalDate,
        @GraphQLDescription("End date of the range (inclusive).") toDate: LocalDate,
        env: DataFetchingEnvironment,
    ): IncomesSummaryGqlDto {
        val incomesService = env.graphQlContext.getBean<IncomesService>()
        val statistics = incomesService.getIncomesStatistics(fromDate, toDate, workspaceId)
        return IncomesSummaryGqlDto(
            items = statistics.map {
                IncomesSummaryItemGqlDto(
                    categoryId = it.categoryId,
                    totalAmount = it.totalAmount,
                    finalizedCount = it.finalizedCount,
                    pendingCount = it.pendingCount,
                    currencyExchangeDifference = it.currencyExchangeDifference,
                )
            }
        )
    }

    @GraphQLDescription("Summary of income tax payments in the given date range.")
    suspend fun incomeTaxPaymentsSummary(
        @GraphQLDescription("Start date of the range (inclusive).") fromDate: LocalDate,
        @GraphQLDescription("End date of the range (inclusive).") toDate: LocalDate,
        env: DataFetchingEnvironment,
    ): IncomeTaxPaymentsSummaryGqlDto {
        val incomeTaxPaymentService = env.graphQlContext.getBean<IncomeTaxPaymentService>()
        val statistics = incomeTaxPaymentService.getTaxPaymentStatistics(fromDate, toDate, workspaceId)
        return IncomeTaxPaymentsSummaryGqlDto(totalTaxPayments = statistics.totalTaxPayments)
    }

    @GraphQLDescription("Shortlist of recently used currency codes, sorted by usage frequency.")
    suspend fun currenciesShortlist(env: DataFetchingEnvironment): List<String> {
        val workspacesService = env.graphQlContext.getBean<WorkspacesService>()
        val expenseService = env.graphQlContext.getBean<ExpenseService>()
        val incomesService = env.graphQlContext.getBean<IncomesService>()
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

@GraphQLName("ExpensesSummary")
@GraphQLDescription("Summary of expenses for a date range.")
data class ExpensesSummaryGqlDto(
    @GraphQLDescription("Per-category breakdown of expenses.")
    val items: List<ExpensesSummaryItemGqlDto>,
) {
    @GraphQLDescription("Total amount of all finalized expenses in the range.")
    val totalAmount: Long = items.sumOf { it.totalAmount }

    @GraphQLDescription("Number of finalized expenses in the range.")
    val finalizedCount: Long = items.sumOf { it.finalizedCount }

    @GraphQLDescription("Number of pending expenses in the range.")
    val pendingCount: Long = items.sumOf { it.pendingCount }

    @GraphQLDescription("Total currency exchange difference for finalized expenses in the range.")
    val currencyExchangeDifference: Long = items.sumOf { it.currencyExchangeDifference }
}

@GraphQLName("ExpensesSummaryItem")
@GraphQLDescription("Expenses summary for a single category.")
data class ExpensesSummaryItemGqlDto(
    @GraphQLIgnore val categoryId: Long?,
    @GraphQLDescription("Total amount for this category.")
    val totalAmount: Long,
    @GraphQLDescription("Number of finalized expenses.")
    val finalizedCount: Long,
    @GraphQLDescription("Number of pending expenses.")
    val pendingCount: Long,
    @GraphQLDescription("Currency exchange difference for finalized expenses.")
    val currencyExchangeDifference: Long,
) {
    @GraphQLDescription("Category of the expenses, or null if no category.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val id = categoryId ?: return null
        return env.loadCategoryById(id)
    }
}

@GraphQLName("IncomesSummary")
@GraphQLDescription("Summary of incomes for a date range.")
data class IncomesSummaryGqlDto(
    @GraphQLDescription("Per-category breakdown of incomes.")
    val items: List<IncomesSummaryItemGqlDto>,
) {
    @GraphQLDescription("Total amount of all finalized incomes in the range.")
    val totalAmount: Long = items.sumOf { it.totalAmount }

    @GraphQLDescription("Number of finalized incomes in the range.")
    val finalizedCount: Long = items.sumOf { it.finalizedCount }

    @GraphQLDescription("Number of pending incomes in the range.")
    val pendingCount: Long = items.sumOf { it.pendingCount }

    @GraphQLDescription("Total currency exchange difference for finalized incomes in the range.")
    val currencyExchangeDifference: Long = items.sumOf { it.currencyExchangeDifference }
}

@GraphQLName("IncomesSummaryItem")
@GraphQLDescription("Incomes summary for a single category.")
data class IncomesSummaryItemGqlDto(
    @GraphQLIgnore val categoryId: Long?,
    @GraphQLDescription("Total amount for this category.")
    val totalAmount: Long,
    @GraphQLDescription("Number of finalized incomes.")
    val finalizedCount: Long,
    @GraphQLDescription("Number of pending incomes.")
    val pendingCount: Long,
    @GraphQLDescription("Currency exchange difference for finalized incomes.")
    val currencyExchangeDifference: Long,
) {
    @GraphQLDescription("Category of the incomes, or null if no category.")
    fun category(env: DataFetchingEnvironment): CompletableFuture<CategoryGqlDto?>? {
        val id = categoryId ?: return null
        return env.loadCategoryById(id)
    }
}

@GraphQLName("IncomeTaxPaymentsSummary")
@GraphQLDescription("Summary of income tax payments for a date range.")
data class IncomeTaxPaymentsSummaryGqlDto(
    @GraphQLDescription("Total amount of all income tax payments in the range.")
    val totalTaxPayments: Long,
)
