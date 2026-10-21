package io.orangebuffalo.simpleaccounting.business.integrations.wise

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

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
            } catch (e: Exception) {
                // Do not include exception messages or causes: they may contain credentials or statement data.
                logger.error { "Unexpected Wise statement fetch failure for integration ${integration.id} (${e.javaClass.name}); continuing with other integrations" }
            }
        }
    }
}
