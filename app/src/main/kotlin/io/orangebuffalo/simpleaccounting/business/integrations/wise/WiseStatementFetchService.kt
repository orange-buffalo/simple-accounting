package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecordsRepository
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementInvoiceLinkingService
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementInvoiceLinkingService.InvoiceMatchingResult
import io.orangebuffalo.simpleaccounting.infra.TimeService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@Service
class WiseStatementFetchService internal constructor(
    private val settings: WiseIntegrationSettingsRepository,
    private val client: WiseApiClient,
    private val records: AccountStatementRecordsRepository,
    private val time: TimeService,
    private val invoiceLinking: AccountStatementInvoiceLinkingService,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = KotlinLogging.logger {}
    private val fetchTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    @Scheduled(cron = "0 0 */4 * * *", zone = "UTC")
    fun fetchStatements() {
        val started = System.nanoTime()
        var selected = 0
        var processed = 0
        var completed = false
        logger.info { "Wise scheduled statement fetching started" }
        try {
            val integrations = settings.findAllByFetchTransactionsPeriodicallyTrueAndAuthenticationRequiredFalse()
            selected = integrations.size
            logger.info { "Wise scheduled statement fetching selected $selected integrations" }
            integrations.forEach { integration ->
                fetch(integration.id!!)
                processed++
            }
            completed = true
        } finally {
            logger.info {
                "Wise scheduled statement fetching finished: outcome=${if (completed) "SUCCESS" else "FAILED"}, " +
                    "selected=$selected, processed=$processed, elapsedMs=${elapsedMillis(started)}"
            }
        }
    }

    @Async
    fun fetchAsync(integrationId: String) {
        fetch(integrationId)
    }

    fun fetch(integrationId: String) {
        val started = System.nanoTime()
        var outcome = FetchOutcome.FAILED
        logger.info { "Wise statement fetch started: integration=$integrationId" }
        try {
            val result = fetchTransaction.execute { fetchInTransaction(integrationId) }!!
            outcome = result.outcome
            if (outcome == FetchOutcome.SUCCESS) {
                logger.info {
                    "Wise statement import committed: integration=$integrationId, fetchedRecords=${result.fetched}, " +
                        "importedRecords=${result.imported}, duplicateRecords=${result.fetched - result.imported}, " +
                        "invoiceMatchingChecked=${result.matching.checked}, linkedInvoices=${result.matching.links.size}, " +
                        "ambiguousInvoiceMatches=${result.matching.ambiguous}"
                }
                result.matching.links.forEach { link ->
                    logger.info { "Wise statement invoice link committed: integration=$integrationId, statement=${link.statementId}, invoice=${link.invoiceId}" }
                }
            }
        } catch (_: WiseUnavailableException) {
            outcome = FetchOutcome.PROVIDER_FAILURE
            logger.warn { "Wise statement fetch failed for integration $integrationId; will retry on the next run" }
        } catch (e: Exception) {
            logger.error(e) { "Unexpected Wise statement fetch failure for integration $integrationId" }
        } finally {
            logger.info { "Wise statement fetch finished: integration=$integrationId, outcome=$outcome, elapsedMs=${elapsedMillis(started)}" }
        }
    }

    private fun fetchInTransaction(integrationId: String): FetchResult {
        // Serializes fetches and setup changes across application instances, including their checkpoints.
        val integration = settings.lockById(integrationId) ?: return FetchResult(FetchOutcome.MISSING)
        if (!integration.fetchTransactionsPeriodically) return FetchResult(FetchOutcome.DISABLED)
        if (integration.authenticationRequired) return FetchResult(FetchOutcome.AUTHENTICATION_REQUIRED)
        if (integration.accounts.isEmpty()) return FetchResult(FetchOutcome.NO_ACCOUNTS)
        logger.info { "Wise statement fetch processing integration=$integrationId, selectedAccounts=${integration.accounts.size}" }
        val end = time.currentTime()
        val fetched = try {
            integration.accounts.flatMap { account ->
                fetchAccount(integration = integration, account = account, end = end)
            }
        } catch (_: WiseInvalidTokenException) {
            settings.save(integration.copy(authenticationRequired = true))
            logger.warn {
                "Wise statement access rejected: integration=$integrationId; suspending fetching until access is restored. " +
                    "Statements may require SCA or additional account permissions; personal-token statement access is limited by account region."
            }
            return FetchResult(FetchOutcome.AUTHENTICATION_REQUIRED)
        }
        var imported = 0
        fetched.forEach { record ->
            val existing = records.findByIntegrationIdAndAccountIdAndExternalId(
                integrationId = record.integrationId, accountId = record.accountId, externalId = record.externalId,
            )
            if (existing == null) {
                records.save(record)
                imported++
            }
        }
        settings.save(integration.copy(
            lastSuccessfulFetchTime = end,
            accounts = integration.accounts.map { it.copy(lastSuccessfulFetchTime = end) }.toSet(),
        ))
        val matching = invoiceLinking.linkInvoices(integration.workspaceId)
        return FetchResult(outcome = FetchOutcome.SUCCESS, fetched = fetched.size, imported = imported, matching = matching)
    }

    private fun fetchAccount(integration: WiseIntegrationSettings, account: WiseIntegrationAccount, end: Instant): List<AccountStatementRecord> {
        val started = System.nanoTime()
        var start = account.lastSuccessfulFetchTime?.minus(Duration.ofDays(7))
            ?: end.atZone(ZoneOffset.UTC).minusMonths(3).toInstant()
        val context = "integration=${integration.id}, profile=${account.profileId}, account=${account.accountId}, currency=${account.currency}"
        var fetched = 0
        var windows = 0
        var outcome = FetchOutcome.FAILED
        logger.info { "Wise account statement fetch started: $context, intervalStart=$start, intervalEnd=$end" }
        try {
            val records = buildList<AccountStatementRecord> {
                // Wise limits a statement to 469 days; bounded windows also cover long disabled periods.
                while (start < end) {
                    val windowEnd = minOf(a = start.plus(Duration.ofDays(365)), b = end)
                    val statement = client.getStatement(settings = integration, account = account, start = start, end = windowEnd)
                    addAll(statement)
                    fetched += statement.size
                    windows++
                    logger.debug { "Wise account statement window fetched: $context, intervalStart=$start, intervalEnd=$windowEnd, fetchedRecords=${statement.size}" }
                    start = windowEnd
                }
            }
            outcome = FetchOutcome.SUCCESS
            return records
        } finally {
            logger.info {
                "Wise account statement fetch finished: $context, outcome=$outcome, completedWindows=$windows, fetchedRecords=$fetched, " +
                    "committed=false, elapsedMs=${elapsedMillis(started)}"
            }
        }
    }

    private fun elapsedMillis(started: Long): Long = (System.nanoTime() - started) / 1_000_000

    private data class FetchResult(
        val outcome: FetchOutcome,
        val fetched: Int = 0,
        val imported: Int = 0,
        val matching: InvoiceMatchingResult = InvoiceMatchingResult(),
    )

    private enum class FetchOutcome { SUCCESS, MISSING, DISABLED, AUTHENTICATION_REQUIRED, NO_ACCOUNTS, PROVIDER_FAILURE, FAILED }
}
