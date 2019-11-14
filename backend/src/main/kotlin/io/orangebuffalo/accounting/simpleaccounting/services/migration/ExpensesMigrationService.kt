package io.orangebuffalo.accounting.simpleaccounting.services.migration

import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class ExpensesMigrationService(
    private val expenseService: ExpenseService,
    private val expenseRepository: ExpenseRepository
) {

    @EventListener
    @Transactional
    fun onApplicationReady(applicationReadyEvent: ApplicationReadyEvent) =
        runBlocking(newSingleThreadContext("expenses-migration")) {
            // spring boot does not yet support coroutines in event listeners
            // thus must force the flow to be executed in a single thread to re-use the transaction

            logger.info { "starting migration of expenses" }

            expenseRepository.findAll().forEach { expense ->
                logger.info { "migrating $expense" }

                expenseService.saveExpense(expense)

                expenseRepository.flush()
            }

            logger.info { "expenses migration finished" }
        }
}
