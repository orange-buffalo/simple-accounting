package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecordsRepository
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.infra.TimeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.ZoneOffset

@Service
class WiseStatementFetchService internal constructor(
    private val settings: WiseIntegrationSettingsRepository,
    private val client: WiseApiClient,
    private val records: AccountStatementRecordsRepository,
    private val time: TimeService,
) {
    @Transactional
    fun fetch(integrationId: String) {
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
            val existing = records.findByProviderAndIntegrationIdAndProfileIdAndAccountIdAndAccountCurrencyAndExternalIdAndDirection(
                provider = record.provider, integrationId = record.integrationId, profileId = record.profileId,
                accountId = record.accountId, accountCurrency = record.accountCurrency, externalId = record.externalId, direction = record.direction,
            )
            if (existing == null) records.save(record)
        }
        settings.save(integration.copy(
            lastSuccessfulFetchTime = end,
            accounts = integration.accounts.map { it.copy(lastSuccessfulFetchTime = end) }.toSet(),
        ))
    }
}
