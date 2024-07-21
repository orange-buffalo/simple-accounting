package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.domain.categories.CategoryService
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
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
    private val categoryService: CategoryService,
    private val documentsService: DocumentsService
) {

    /**
     * Re-calculates the expense state (denormalized presentation).
     */
    suspend fun saveExpense(expense: Expense): Expense {
        val workspace = workspaceService.getAccessibleWorkspace(expense.workspaceId, WorkspaceAccessMode.READ_WRITE)
        validateCategoryAndAttachments(expense, workspace.id!!)

        val defaultCurrency = workspace.defaultCurrency
        if (defaultCurrency == expense.currency) {
            expense.convertedAmounts = AmountsInDefaultCurrency(expense.originalAmount, null)
            expense.incomeTaxableAmounts = AmountsInDefaultCurrency(expense.originalAmount, null)
            expense.useDifferentExchangeRateForIncomeTaxPurposes = false
        }

        if (!expense.useDifferentExchangeRateForIncomeTaxPurposes) {
            expense.incomeTaxableAmounts = expense.convertedAmounts
        }

        val generalTax = getGeneralTax(expense)
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

        return withDbContext { expenseRepository.save(expense) }
    }

    private suspend fun getGeneralTax(expense: Expense): GeneralTax? =
        if (expense.generalTaxId == null) null else generalTaxService.getValidGeneralTax(
            expense.generalTaxId!!,
            expense.workspaceId
        )

    private suspend fun validateCategoryAndAttachments(
        expense: Expense,
        workspaceId: Long
    ) = executeInParallel {
        step { validateCategory(expense, workspaceId) }
        step { validateAttachments(expense, workspaceId) }
    }

    private suspend fun validateAttachments(expense: Expense, workspaceId: Long) {
        if (expense.attachments.isNotEmpty()) {
            val attachmentsIds = expense.attachments.map { it.documentId }
            documentsService.validateDocuments(workspaceId, attachmentsIds)
        }
    }

    private suspend fun validateCategory(
        expense: Expense,
        workspaceId: Long
    ) {
        if (expense.categoryId != null) categoryService.validateCategory(expense.categoryId!!, workspaceId)
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

    suspend fun getExpenseByIdAndWorkspace(id: Long, workspaceId: Long): Expense? = withDbContext {
        expenseRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun getExpensesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<ExpensesStatistics> = withDbContext {
        expenseRepository.getStatistics(fromDate, toDate, workspaceId)
    }

    suspend fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics> = withDbContext {
        expenseRepository.getCurrenciesUsageStatistics(workspace)
    }

    private data class AdjustedAmounts(
        val generalTaxAmount: Long?,
        val adjustedAmount: Long?
    )
}
