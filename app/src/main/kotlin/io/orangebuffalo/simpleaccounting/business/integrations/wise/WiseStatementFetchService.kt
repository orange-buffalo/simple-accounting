package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecordsRepository
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.infra.TimeService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.ZoneOffset

@Service
class WiseStatementFetchService internal constructor(
    private val settings: WiseIntegrationSettingsRepository,
    private val client: WiseApiClient,
    private val records: AccountStatementRecordsRepository,
    private val time: TimeService,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = KotlinLogging.logger {}
    private val fetchTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    @Scheduled(cron = "0 0 */4 * * *", zone = "UTC")
    fun fetchStatements() {
        settings.findAllByFetchTransactionsPeriodicallyTrueAndAuthenticationRequiredFalse().forEach { integration ->
            fetch(integration.id!!)
        }
    }

    @Async
    fun fetchAsync(integrationId: String) {
        fetch(integrationId)
    }

    fun fetch(integrationId: String) {
        try {
            fetchTransaction.executeWithoutResult { fetchInTransaction(integrationId) }
        } catch (_: WiseUnavailableException) {
            logger.warn { "Wise statement fetch failed for integration $integrationId; will retry on the next run" }
        } catch (e: Exception) {
            logger.error(e) { "Unexpected Wise statement fetch failure for integration $integrationId" }
        }
    }

    private fun fetchInTransaction(integrationId: String) {
        // Serializes fetches and setup changes across application instances, including their checkpoints.
        val integration = settings.lockById(integrationId) ?: return
        if (!integration.fetchTransactionsPeriodically || integration.authenticationRequired || integration.accounts.isEmpty()) return
        val end = time.currentTime()
        val fetched = try {
            integration.accounts.flatMap { account ->
                var start = account.lastSuccessfulFetchTime?.minus(Duration.ofDays(7))
                    ?: end.atZone(ZoneOffset.UTC).minusMonths(3).toInstant()
                buildList<AccountStatementRecord> {
                    // Wise limits a statement to 469 days; bounded windows also cover long disabled periods.
                    while (start < end) {
                        val windowEnd = minOf(a = start.plus(Duration.ofDays(365)), b = end)
                        addAll(client.getStatement(settings = integration, account = account, start = start, end = windowEnd))
                        start = windowEnd
                    }
                }
            }
        } catch (_: WiseInvalidTokenException) {
            settings.save(integration.copy(authenticationRequired = true))
            return
        }
        fetched.forEach { record ->
            val existing = records.findByIntegrationIdAndAccountIdAndExternalId(
                integrationId = record.integrationId, accountId = record.accountId, externalId = record.externalId,
            )
            if (existing == null) records.save(record)
        }
        settings.save(integration.copy(
            lastSuccessfulFetchTime = end,
            accounts = integration.accounts.map { it.copy(lastSuccessfulFetchTime = end) }.toSet(),
        ))
    }
}
