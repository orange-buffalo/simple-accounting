package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {

    suspend fun saveExpense(expense: Expense): Expense = withDbContext {
        expenseRepository.save(expense)
    }

    suspend fun getExpenses(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Expense> = withDbContext {
        expenseRepository.findAll(QExpense.expense.category.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getExpenseByIdAndWorkspace(id: Long, workspace: Workspace): Expense? = withDbContext {
        expenseRepository.findByIdAndCategoryWorkspace(id, workspace)
    }
}