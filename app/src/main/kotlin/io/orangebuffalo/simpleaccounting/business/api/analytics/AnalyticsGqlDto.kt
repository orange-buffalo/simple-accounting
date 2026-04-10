package io.orangebuffalo.simpleaccounting.business.api.analytics

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.categories.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.api.categories.loadCategoryById
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.GeneralTaxGqlDto
import io.orangebuffalo.simpleaccounting.business.api.generaltaxes.loadGeneralTaxByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.analytics.WorkspaceAnalyticsService
import io.orangebuffalo.simpleaccounting.business.expenses.ExpenseService
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesReportingService
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

    @GraphQLDescription("Summary of general taxes in the given date range.")
    suspend fun generalTaxesSummary(
        @GraphQLDescription("Start date of the range (inclusive).") fromDate: LocalDate,
        @GraphQLDescription("End date of the range (inclusive).") toDate: LocalDate,
        env: DataFetchingEnvironment,
    ): GeneralTaxesSummaryGqlDto {
        val workspacesService = env.graphQlContext.getBean<WorkspacesService>()
        val taxReportingService = env.graphQlContext.getBean<GeneralTaxesReportingService>()
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val report = taxReportingService.getGeneralTaxReport(fromDate, toDate, workspace)
        return GeneralTaxesSummaryGqlDto(
            workspaceId = workspaceId,
            finalizedCollectedTaxes = report.finalizedCollectedTaxes.map {
                FinalizedGeneralTaxSummaryItemGqlDto(
                    workspaceId = workspaceId,
                    taxId = it.tax,
                    taxAmount = it.taxAmount,
                    includedItemsNumber = it.includedItemsNumber,
                    includedItemsAmount = it.includedItemsAmount,
                )
            },
            finalizedPaidTaxes = report.finalizedPaidTaxes.map {
                FinalizedGeneralTaxSummaryItemGqlDto(
                    workspaceId = workspaceId,
                    taxId = it.tax,
                    taxAmount = it.taxAmount,
                    includedItemsNumber = it.includedItemsNumber,
                    includedItemsAmount = it.includedItemsAmount,
                )
            },
            pendingCollectedTaxes = report.pendingCollectedTaxes.map {
                PendingGeneralTaxSummaryItemGqlDto(
                    workspaceId = workspaceId,
                    taxId = it.tax,
                    includedItemsNumber = it.includedItemsNumber,
                )
            },
            pendingPaidTaxes = report.pendingPaidTaxes.map {
                PendingGeneralTaxSummaryItemGqlDto(
                    workspaceId = workspaceId,
                    taxId = it.tax,
                    includedItemsNumber = it.includedItemsNumber,
                )
            },
        )
    }

    @GraphQLDescription("Shortlist of recently used currency codes, sorted by usage frequency.")
    suspend fun currenciesShortlist(env: DataFetchingEnvironment): List<String> {
        val workspacesService = env.graphQlContext.getBean<WorkspacesService>()
        val analyticsService = env.graphQlContext.getBean<WorkspaceAnalyticsService>()
        val workspace = workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        return analyticsService.getCurrenciesShortlist(workspace)
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

@GraphQLName("GeneralTaxesSummary")
@GraphQLDescription("Summary of general taxes for a date range.")
data class GeneralTaxesSummaryGqlDto(
    @GraphQLIgnore val workspaceId: Long,
    @GraphQLDescription("Finalized taxes collected on incomes.")
    val finalizedCollectedTaxes: List<FinalizedGeneralTaxSummaryItemGqlDto>,
    @GraphQLDescription("Finalized taxes paid on expenses.")
    val finalizedPaidTaxes: List<FinalizedGeneralTaxSummaryItemGqlDto>,
    @GraphQLDescription("Pending taxes to be collected on incomes.")
    val pendingCollectedTaxes: List<PendingGeneralTaxSummaryItemGqlDto>,
    @GraphQLDescription("Pending taxes to be paid on expenses.")
    val pendingPaidTaxes: List<PendingGeneralTaxSummaryItemGqlDto>,
)

@GraphQLName("FinalizedGeneralTaxSummaryItem")
@GraphQLDescription("Summary of a finalized general tax.")
data class FinalizedGeneralTaxSummaryItemGqlDto(
    @GraphQLIgnore val workspaceId: Long,
    @GraphQLIgnore val taxId: Long,
    @GraphQLDescription("Total amount of tax collected or paid.")
    val taxAmount: Long,
    @GraphQLDescription("Number of items contributing to this tax.")
    val includedItemsNumber: Long,
    @GraphQLDescription("Total amount of items contributing to this tax.")
    val includedItemsAmount: Long,
) {
    @GraphQLDescription("The general tax.")
    fun tax(env: DataFetchingEnvironment): CompletableFuture<GeneralTaxGqlDto?> =
        env.loadGeneralTaxByWorkspaceAndId(workspaceId, taxId)
}

@GraphQLName("PendingGeneralTaxSummaryItem")
@GraphQLDescription("Summary of a pending general tax.")
data class PendingGeneralTaxSummaryItemGqlDto(
    @GraphQLIgnore val workspaceId: Long,
    @GraphQLIgnore val taxId: Long,
    @GraphQLDescription("Number of items contributing to this tax.")
    val includedItemsNumber: Long,
) {
    @GraphQLDescription("The general tax.")
    fun tax(env: DataFetchingEnvironment): CompletableFuture<GeneralTaxGqlDto?> =
        env.loadGeneralTaxByWorkspaceAndId(workspaceId, taxId)
}
