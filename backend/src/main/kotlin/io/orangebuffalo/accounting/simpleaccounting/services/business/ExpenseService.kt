package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpensesStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {

    /**
     * If tax is provided, original amount always includes the tax.
     */
    suspend fun saveExpense(expense: Expense): Expense {
        val defaultCurrency = expense.workspace.defaultCurrency
        if (defaultCurrency == expense.currency) {
            expense.amountInDefaultCurrency = expense.originalAmount
            expense.actualAmountInDefaultCurrency = expense.originalAmount
        } else if (expense.amountInDefaultCurrency == 0L) {
            expense.actualAmountInDefaultCurrency = 0
        }

        val generalTax = expense.generalTax
        expense.generalTaxRateInBps = generalTax?.rateInBps

        val actualAmountOnBusiness = expense.actualAmountInDefaultCurrency.percentPart(expense.percentOnBusiness)

        val baseAmountForAddedGeneralTax = if (generalTax == null) {
            actualAmountOnBusiness
        } else {
            actualAmountOnBusiness.bpsBasePart(generalTax.rateInBps)
        }

        expense.reportedAmountInDefaultCurrency = baseAmountForAddedGeneralTax
        expense.generalTaxAmount = if (generalTax == null) {
            null
        } else {
            actualAmountOnBusiness - baseAmountForAddedGeneralTax
        }

        return withDbContext {
            expenseRepository.save(expense)
        }
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
