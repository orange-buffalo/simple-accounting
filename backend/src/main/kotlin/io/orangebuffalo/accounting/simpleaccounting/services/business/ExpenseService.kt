package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.registerEntitySaveListener
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpensesStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val entityManagerFactory: EntityManagerFactory
) {

    @PostConstruct
    private fun initPersistenceListeners() {
        entityManagerFactory.registerEntitySaveListener(::validateExpenseConsistency)
    }

    /**
     * Sanity check that an [Expense] is consistent (i.e. all the denormalized fields
     * are compatible and plausible). Only most critical verifications are provided.
     */
    fun validateExpenseConsistency(expense: Expense) {
        val isDefaultCurrency = expense.currency == expense.workspace.defaultCurrency
        if (isDefaultCurrency) {
            require(expense.originalAmount == expense.convertedAmounts.originalAmountInDefaultCurrency) {
                "Inconsistent expense: converted amount does not match original for default currency"
            }

            require(expense.originalAmount == expense.incomeTaxableAmounts.originalAmountInDefaultCurrency) {
                "Inconsistent expense: income taxable amount does not match original for default currency"
            }
        }

        if (!expense.useDifferentExchangeRateForIncomeTaxPurposes) {
            require(expense.convertedAmounts == expense.incomeTaxableAmounts) {
                "Inconsistent expense: amounts do not match but same exchange rate is used"
            }
        }

        if (expense.status == ExpenseStatus.FINALIZED) {
            require(expense.convertedAmounts.notEmpty && expense.incomeTaxableAmounts.notEmpty) {
                "Inconsistent expense: amounts are not provided for finalized expense"
            }
        }
    }

    /**
     * Re-calculates the expense state (denormalized presentation).
     */
    suspend fun saveExpense(expense: Expense): Expense {
        val defaultCurrency = expense.workspace.defaultCurrency
        if (defaultCurrency == expense.currency) {
            expense.convertedAmounts = AmountsInDefaultCurrency(expense.originalAmount, null)
            expense.incomeTaxableAmounts = AmountsInDefaultCurrency(expense.originalAmount, null)
            expense.useDifferentExchangeRateForIncomeTaxPurposes = false
        }

        if (!expense.useDifferentExchangeRateForIncomeTaxPurposes) {
            expense.incomeTaxableAmounts = expense.convertedAmounts
        }

        val generalTax = expense.generalTax
        expense.generalTaxRateInBps = generalTax?.rateInBps

        val convertedAdjustedAmounts = calculateAdjustedAmounts(expense, expense.convertedAmounts)
        expense.convertedAmounts.adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmounts(expense, expense.incomeTaxableAmounts)
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

    private fun calculateAdjustedAmounts(expense: Expense, targetAmounts: AmountsInDefaultCurrency): AdjustedAmounts {
        val originalAmountInDefaultCurrency = targetAmounts.originalAmountInDefaultCurrency
            ?: return AdjustedAmounts(null, null)

        val amountOnBusinessPurposes = originalAmountInDefaultCurrency.percentPart(expense.percentOnBusiness)

        val generalTax = expense.generalTax
            ?: return AdjustedAmounts(
                generalTaxAmount = null,
                adjustedAmount = amountOnBusinessPurposes
            )

        val baseAmountForAddedGeneralTax = amountOnBusinessPurposes.bpsBasePart(generalTax.rateInBps)
        return AdjustedAmounts(
            generalTaxAmount = amountOnBusinessPurposes.minus(baseAmountForAddedGeneralTax),
            adjustedAmount = baseAmountForAddedGeneralTax
        )
    }

    suspend fun getExpenses(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Expense> = withDbContext {
        expenseRepository.findAll(QExpense.expense.workspace.eq(workspace).and(filter), page)
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
}

private data class AdjustedAmounts(
    val generalTaxAmount: Long?,
    val adjustedAmount: Long?
)
