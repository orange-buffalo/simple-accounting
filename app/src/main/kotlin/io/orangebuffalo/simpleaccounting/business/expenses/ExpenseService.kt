package io.orangebuffalo.simpleaccounting.business.expenses

import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.services.business.*
import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ExpenseService(
    private val expensesRepository: ExpensesRepository,
    private val workspacesService: WorkspacesService,
    private val generalTaxesService: GeneralTaxesService,
    private val categoriesService: CategoriesService,
    private val documentsService: DocumentsService
) {

    /**
     * Re-calculates the expense state (denormalized presentation).
     */
    suspend fun saveExpense(expense: Expense): Expense {
        val workspace = workspacesService.getAccessibleWorkspace(expense.workspaceId, WorkspaceAccessMode.READ_WRITE)
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

        return withDbContext { expensesRepository.save(expense) }
    }

    private suspend fun getGeneralTax(expense: Expense): GeneralTax? =
        if (expense.generalTaxId == null) null else generalTaxesService.getValidGeneralTax(
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
        if (expense.categoryId != null) categoriesService.validateCategory(expense.categoryId!!, workspaceId)
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
        expensesRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun getExpensesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<ExpensesStatistics> = withDbContext {
        expensesRepository.getStatistics(fromDate, toDate, workspaceId)
    }

    suspend fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics> = withDbContext {
        expensesRepository.getCurrenciesUsageStatistics(workspace)
    }

    private data class AdjustedAmounts(
        val generalTaxAmount: Long?,
        val adjustedAmount: Long?
    )
}
