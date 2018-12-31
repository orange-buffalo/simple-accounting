package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpensesStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {

    suspend fun saveExpense(expense: Expense): Expense {
        val defaultCurrency = expense.category.workspace.defaultCurrency
        if (defaultCurrency == expense.currency) {
            expense.amountInDefaultCurrency = expense.originalAmount
            expense.actualAmountInDefaultCurrency = expense.originalAmount
        }

        expense.reportedAmountInDefaultCurrency = expense.actualAmountInDefaultCurrency.toBigDecimal()
            .multiply(expense.percentOnBusiness.toBigDecimal())
            .divide(100.toBigDecimal(), 0, RoundingMode.HALF_EVEN)
            .longValueExact()

        return withDbContext {
            expenseRepository.save(expense)
        }
    }

    suspend fun getExpenses(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Expense> = withDbContext {
        expenseRepository.findAll(QExpense.expense.category.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getExpenseByIdAndWorkspace(id: Long, workspace: Workspace): Expense? =
        withDbContext {
            expenseRepository.findByIdAndCategoryWorkspace(id, workspace)
        }

    suspend fun getExpensesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): List<ExpensesStatistics> =
        withDbContext {
            expenseRepository.getStatistics(fromDate, toDate, workspace)
        }
}