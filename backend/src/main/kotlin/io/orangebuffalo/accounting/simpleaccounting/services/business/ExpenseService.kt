package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
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

}