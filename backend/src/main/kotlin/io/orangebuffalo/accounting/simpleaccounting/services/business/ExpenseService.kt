package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
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

    suspend fun getExpenses(page: Pageable): Page<Expense> = withDbContext {
        expenseRepository.findAll(page)
    }

    suspend fun getExpense(id: Long): Expense? = withDbContext {
        expenseRepository.findById(id).orElse(null)
    }
}