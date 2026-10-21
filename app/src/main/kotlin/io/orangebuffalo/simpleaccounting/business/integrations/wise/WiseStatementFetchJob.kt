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
            fetchIntegration(integration.id!!)
        }
    }

    fun fetchIntegration(integrationId: String) {
        try {
            fetcher.fetch(integrationId)
        } catch (_: WiseUnavailableException) {
            logger.warn { "Wise statement fetch failed for integration $integrationId; will retry on the next run" }
        } catch (e: Exception) {
            logger.error(e) { "Unexpected Wise statement fetch failure for integration $integrationId; continuing with other integrations" }
        }
    }
}
