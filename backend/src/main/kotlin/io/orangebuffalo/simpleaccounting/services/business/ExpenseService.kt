package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.ExpensesStatistics
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val workspaceService: WorkspaceService,
    private val generalTaxService: GeneralTaxService,
    private val categoryService: CategoryService
) {

    /**
     * Re-calculates the expense state (denormalized presentation).
     */
    suspend fun saveExpense(expense: Expense): Expense {
        val workspace = workspaceService.getAccessibleWorkspace(expense.workspaceId, WorkspaceAccessMode.READ_WRITE)
        val defaultCurrency = workspace.defaultCurrency
        categoryService.getValidCategory(workspace, expense.categoryId)
        // todo #222: validate attachments
        if (defaultCurrency == expense.currency) {
            expense.convertedAmounts = AmountsInDefaultCurrency(expense.originalAmount, null)
            expense.incomeTaxableAmounts = AmountsInDefaultCurrency(expense.originalAmount, null)
            expense.useDifferentExchangeRateForIncomeTaxPurposes = false
        }

        if (!expense.useDifferentExchangeRateForIncomeTaxPurposes) {
            expense.incomeTaxableAmounts = expense.convertedAmounts
        }

        val generalTax = generalTaxService.getValidGeneralTax(expense.generalTaxId, workspace)
        expense.generalTaxRateInBps = generalTax?.rateInBps

        val convertedAdjustedAmounts = calculateAdjustedAmounts(expense, expense.convertedAmounts, generalTax)
        expense.convertedAmounts.adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmounts(expense, expense.incomeTaxableAmounts, generalTax)
        expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency = incomeTaxableAdjustedAmounts.adjustedAmount
        expense.generalTaxAmount = incomeTaxableAdjustedAmounts.generalTaxAmount

        expense.status = when {
            expense.convertedAmounts.adjustedAmountInDefaultCurrency == null -> ExpenseStatus.PENDING_CONVERSION
            expense.incomeTaxableAmounts.adjustedAmountInDefaultCurrency == null ->
                ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
            else -> ExpenseStatus.FINALIZED
        }

        return withDbContext {
            expenseRepository.save(expense)
        }
    }

    private fun calculateAdjustedAmounts(
        expense: Expense,
        targetAmounts: AmountsInDefaultCurrency,
        generalTax: GeneralTax?
    ): AdjustedAmounts {
        val originalAmountInDefaultCurrency = targetAmounts.originalAmountInDefaultCurrency
            ?: return AdjustedAmounts(null, null)

        val amountOnBusinessPurposes = originalAmountInDefaultCurrency.percentPart(expense.percentOnBusiness)

        if (generalTax == null) {
            return AdjustedAmounts(
                generalTaxAmount = null,
                adjustedAmount = amountOnBusinessPurposes
            )
        }

        val baseAmountForAddedGeneralTax = amountOnBusinessPurposes.bpsBasePart(generalTax.rateInBps)
        return AdjustedAmounts(
            generalTaxAmount = amountOnBusinessPurposes.minus(baseAmountForAddedGeneralTax),
            adjustedAmount = baseAmountForAddedGeneralTax
        )
    }

    suspend fun getExpenseByIdAndWorkspace(id: Long, workspace: Workspace): Expense? =
        withDbContext {
            expenseRepository.findByIdAndWorkspace(id, workspace)
        }

    suspend fun getExpensesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics> =
        withDbContext {
            expenseRepository.getStatistics(fromDate, toDate, workspace)
        }

    suspend fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics> = withDbContext {
        expenseRepository.getCurrenciesUsageStatistics(workspace)
    }

    private data class AdjustedAmounts(
        val generalTaxAmount: Long?,
        val adjustedAmount: Long?
    )
}
