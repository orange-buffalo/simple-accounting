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

    fun saveExpense(expense: Expense): Mono<Expense> {
        return Mono.fromSupplier { expenseRepository.save(expense) }
            .subscribeOn(Schedulers.elastic())
    }

    fun getExpenses(page: Pageable): Mono<Page<Expense>> {
        return Mono.fromSupplier { expenseRepository.findAll(page) }
                .subscribeOn(Schedulers.elastic())
    }

}