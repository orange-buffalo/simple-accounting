package io.orangebuffalo.simpleaccounting.business.expenses

import io.orangebuffalo.simpleaccounting.business.categories.CategoriesService
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import io.orangebuffalo.simpleaccounting.business.common.bpsBasePart
import io.orangebuffalo.simpleaccounting.business.common.percentPart
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.executeInParallel
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.business.common.data.CurrenciesUsageStatistics
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

        val isDefaultCurrency = workspace.defaultCurrency == expense.currency
        val convertedAmounts = if (isDefaultCurrency) {
            AmountsInDefaultCurrency(expense.originalAmount, null)
        } else expense.convertedAmounts
        val useDifferentExchangeRateForIncomeTaxPurposes =
            !isDefaultCurrency && expense.useDifferentExchangeRateForIncomeTaxPurposes
        val incomeTaxableAmounts = if (useDifferentExchangeRateForIncomeTaxPurposes) {
            expense.incomeTaxableAmounts
        } else convertedAmounts

        val generalTax = getGeneralTax(expense)
        val convertedAdjustedAmounts = calculateAdjustedAmounts(expense, convertedAmounts, generalTax)
        val adjustedConvertedAmounts = convertedAmounts.copy(
            adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount
        )

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmounts(expense, incomeTaxableAmounts, generalTax)
        val adjustedIncomeTaxableAmounts = incomeTaxableAmounts.copy(
            adjustedAmountInDefaultCurrency = incomeTaxableAdjustedAmounts.adjustedAmount
        )
        val status = when {
            adjustedConvertedAmounts.adjustedAmountInDefaultCurrency == null -> ExpenseStatus.PENDING_CONVERSION
            adjustedIncomeTaxableAmounts.adjustedAmountInDefaultCurrency == null ->
                ExpenseStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
            else -> ExpenseStatus.FINALIZED
        }

        return withDbContext {
            expensesRepository.save(
                expense.copy(
                    convertedAmounts = adjustedConvertedAmounts,
                    incomeTaxableAmounts = adjustedIncomeTaxableAmounts,
                    useDifferentExchangeRateForIncomeTaxPurposes = useDifferentExchangeRateForIncomeTaxPurposes,
                    generalTaxRateInBps = generalTax?.rateInBps,
                    generalTaxAmount = incomeTaxableAdjustedAmounts.generalTaxAmount,
                    status = status,
                )
            )
        }
    }

    private suspend fun getGeneralTax(expense: Expense): GeneralTax? =
        if (expense.generalTaxId == null) null else generalTaxesService.getValidGeneralTax(
            expense.generalTaxId!!,
            expense.workspaceId
        )

    private suspend fun validateCategoryAndAttachments(
        expense: Expense,
        workspaceId: String
    ) = executeInParallel {
        step { validateCategory(expense, workspaceId) }
        step { validateAttachments(expense, workspaceId) }
    }

    private suspend fun validateAttachments(expense: Expense, workspaceId: String) {
        if (expense.attachments.isNotEmpty()) {
            val attachmentsIds = expense.attachments.map { it.documentId }
            documentsService.validateDocuments(workspaceId, attachmentsIds)
        }
    }

    private suspend fun validateCategory(
        expense: Expense,
        workspaceId: String
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

    suspend fun getExpenseByIdAndWorkspace(id: String, workspaceId: String): Expense? = withDbContext {
        expensesRepository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun getExpensesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: String
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
