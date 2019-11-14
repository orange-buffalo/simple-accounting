package io.orangebuffalo.accounting.simpleaccounting.services.migration

import io.orangebuffalo.accounting.simpleaccounting.services.business.IncomeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepository
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class IncomesMigrationService(
    private val incomeService: IncomeService,
    private val incomeRepository: IncomeRepository
) {

    @EventListener
    @Transactional
    fun onApplicationReady(applicationReadyEvent: ApplicationReadyEvent) =
        runBlocking(newSingleThreadContext("incomes-migration")) {
            // spring boot does not yet support coroutines in event listeners
            // thus must force the flow to be executed in a single thread to re-use the transaction

            logger.info { "starting migration of incomes" }

            incomeRepository.findAll().forEach { income ->
                logger.info { "migrating $income" }

                incomeService.saveIncome(income)

                incomeRepository.flush()
            }

            logger.info { "incomes migration finished" }
        }
}
