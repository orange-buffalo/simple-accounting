package io.orangebuffalo.simpleaccounting.business.integrations.wise

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.dao.DataAccessException

@Component
class WiseStatementFetchJob(
    private val settings: WiseIntegrationSettingsRepository,
    private val fetcher: WiseStatementFetchService,
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 */4 * * *", zone = "UTC")
    fun fetchStatements() {
        settings.findAllByFetchTransactionsPeriodicallyTrueAndAuthenticationRequiredFalse().forEach { integration ->
            try {
                fetcher.fetch(integration.id!!)
            } catch (_: WiseUnavailableException) {
                logger.warn { "Wise statement fetch failed for integration ${integration.id}; will retry on the next run" }
            } catch (_: DataAccessException) {
                // Database exception messages can contain credentials or statement data.
                logger.warn { "Wise statement fetch failed for integration ${integration.id}; will retry on the next run" }
            }
        }
    }
}
