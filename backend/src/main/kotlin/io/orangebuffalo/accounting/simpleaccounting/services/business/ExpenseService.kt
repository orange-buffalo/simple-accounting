package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {

    suspend fun saveExpense(expense: Expense): Expense = withDbContext {
        expenseRepository.save(expense)
    }

    @Deprecated("migrate to coroutines")
    fun getExpenses(page: Pageable): Mono<Page<Expense>> {
        return Mono.fromSupplier { expenseRepository.findAll(page) }
            .subscribeOn(Schedulers.elastic())
    }

    @Deprecated("migrate to coroutines")
    fun getExpense(id: Long): Mono<Expense> {
        return Mono.fromSupplier { expenseRepository.findById(id) }
            .filter { it.isPresent }
            .map { it.get() }
            .subscribeOn(Schedulers.elastic())
    }
}