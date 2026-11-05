package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecordsRepository
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementAttachment
import io.orangebuffalo.simpleaccounting.business.integrations.IntegrationProvider
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocumentsRepository
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.inputStreamProvider
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementInvoiceLinkingService
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementInvoiceLinkingService.InvoiceMatchingResult
import io.orangebuffalo.simpleaccounting.infra.TimeService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.YearMonth

@Service
class WiseStatementFetchService internal constructor(
    private val settings: WiseIntegrationSettingsRepository,
    private val client: WiseApiClient,
    private val records: AccountStatementRecordsRepository,
    private val time: TimeService,
    private val invoiceLinking: AccountStatementInvoiceLinkingService,
    private val documents: DocumentsService,
    private val standaloneDocuments: StandaloneDocumentsRepository,
    private val workspaces: WorkspacesService,
    private val users: PlatformUsersService,
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
            val integrations = settings.findAllWithAutomaticFetchingEnabled()
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
            val result = inIntegrationTransaction(integrationId = integrationId) { fetchInTransaction(it) }
                ?: FetchResult(FetchOutcome.MISSING)
            outcome = result.outcome
            if (outcome == FetchOutcome.SUCCESS) {
                if (result.transactionsFetched) {
                    logger.info {
                        "Wise statement import committed: integration=$integrationId, fetchedRecords=${result.fetched}, " +
                            "importedRecords=${result.imported}, duplicateRecords=${result.fetched - result.imported}"
                    }
                    attachMissingDocuments(integrationId)
                }
                if (result.transactionsFetched) {
                    val matching = inIntegrationTransaction(integrationId = integrationId) { invoiceLinking.linkInvoices(it.workspaceId) }
                        ?: InvoiceMatchingResult()
                    logger.info {
                        "Wise statement invoice matching committed: integration=$integrationId, checked=${matching.checked}, " +
                            "linkedInvoices=${matching.links.size}, ambiguousInvoiceMatches=${matching.ambiguous}"
                    }
                    matching.links.forEach { link ->
                        logger.info { "Wise statement invoice link committed: integration=$integrationId, statement=${link.statementId}, invoice=${link.invoiceId}" }
                    }
                }
                fetchMonthlyStatements(integrationId)
            }
        } catch (_: WiseInvalidTokenException) {
            outcome = FetchOutcome.AUTHENTICATION_REQUIRED
            logger.warn {
                "Wise statement access rejected: integration=$integrationId. " +
                    "Statements may require SCA or additional account permissions; personal-token statement access is limited by account region."
            }
        } catch (_: WiseUnavailableException) {
            outcome = FetchOutcome.PROVIDER_FAILURE
            logger.warn { "Wise statement fetch failed for integration $integrationId; will retry on the next run" }
        } catch (e: Exception) {
            outcome = FetchOutcome.FAILED
            logger.error(e) { "Unexpected Wise statement fetch failure for integration $integrationId" }
        } finally {
            logger.info { "Wise statement fetch finished: integration=$integrationId, outcome=$outcome, elapsedMs=${elapsedMillis(started)}" }
        }
    }

    /**
     * Runs the operation in an independent transaction under the integration lock, serializing fetches and setup changes.
     * On rejected credentials, rolls back the operation and marks authentication required in a separate transaction,
     * but only if the attempted settings version is still current, so newer credentials are not invalidated.
     */
    private fun <T> inIntegrationTransaction(integrationId: String, operation: (WiseIntegrationSettings) -> T): T? {
        var attemptedVersion: Int? = null
        try {
            return fetchTransaction.execute {
                val integration = settings.lockById(integrationId) ?: return@execute null
                attemptedVersion = integration.version
                operation(integration)
            }
        } catch (e: WiseInvalidTokenException) {
            fetchTransaction.executeWithoutResult {
                settings.lockById(integrationId)?.takeIf { it.version == attemptedVersion }
                    ?.let { settings.save(it.copy(authenticationRequired = true)) }
            }
            throw e
        }
    }

    private fun fetchInTransaction(integration: WiseIntegrationSettings): FetchResult {
        // Serializes fetches and setup changes across application instances, including their checkpoints.
        val integrationId = integration.id!!
        if (!integration.fetchTransactionsPeriodically && !integration.fetchMonthlyStatements) return FetchResult(FetchOutcome.DISABLED)
        if (integration.authenticationRequired) return FetchResult(FetchOutcome.AUTHENTICATION_REQUIRED)
        if (integration.accounts.isEmpty()) return FetchResult(FetchOutcome.NO_ACCOUNTS)
        if (!integration.fetchTransactionsPeriodically) return FetchResult(FetchOutcome.SUCCESS)
        logger.info { "Wise statement fetch processing integration=$integrationId, selectedAccounts=${integration.accounts.size}" }
        val end = time.currentTime()
        val fetched = integration.accounts.flatMap { account ->
            fetchAccount(integration = integration, account = account, end = end)
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
        return FetchResult(outcome = FetchOutcome.SUCCESS, fetched = fetched.size, imported = imported, transactionsFetched = true)
    }

    private fun attachMissingDocuments(integrationId: String) {
        val integration = settings.findByIdOrNull(integrationId) ?: return
        val workspace = workspaces.getWorkspace(integration.workspaceId)
        val owner = users.getUserByUserId(workspace.ownerId)
        runAs(owner.toSecurityPrincipal()) {
            if (!documents.getCurrentUserStorageStatus().active) return@runAs
            records.findWithoutDocuments(
                workspaceId = workspace.id!!, provider = IntegrationProvider.WISE.name, integrationId = integration.id!!,
            ).forEach { record ->
                inIntegrationTransaction(integrationId = integrationId) { currentIntegration ->
                    if (!currentIntegration.fetchTransactionsPeriodically || currentIntegration.authenticationRequired) return@inIntegrationTransaction
                    if (!documents.getCurrentUserStorageStatus().active) return@inIntegrationTransaction
                    val currentRecord = records.findByIdOrNull(record.id!!) ?: return@inIntegrationTransaction
                    if (currentRecord.attachments.isNotEmpty()) return@inIntegrationTransaction
                    client.getStatementPdf(settings = currentIntegration, record = currentRecord) { fileName, content ->
                        val document = documents.saveDocument(SaveDocumentRequest(
                            fileName = fileName,
                            content = inputStreamProvider { content },
                            workspace = workspace,
                            contentType = "application/pdf",
                        ))
                        records.save(currentRecord.copy(attachments = setOf(AccountStatementAttachment(document.id!!))))
                    }
                }
            }
        }
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

    private fun fetchMonthlyStatements(integrationId: String) {
        val integration = settings.findByIdOrNull(integrationId) ?: return
        if (!integration.fetchMonthlyStatements || integration.authenticationRequired) return
        val workspace = workspaces.getWorkspace(integration.workspaceId)
        val owner = users.getUserByUserId(workspace.ownerId)
        val month = YearMonth.from(time.currentTime().atZone(ZoneOffset.UTC)).minusMonths(1)
        val firstDate = month.atDay(1)
        val lastDate = month.atEndOfMonth()
        val start = firstDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = lastDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1)
        runAs(owner.toSecurityPrincipal()) {
            if (!documents.getCurrentUserStorageStatus().active) return@runAs
            integration.accounts.forEach { account ->
                val started = System.nanoTime()
                val context = "integration=$integrationId, profile=${account.profileId}, account=${account.accountId}, currency=${account.currency}"
                var outcome = "FAILED"
                logger.info { "Wise monthly statement download started: $context, intervalStart=$start, intervalEnd=$end" }
                try {
                    val title = "Wise Monthly Statement for account ${account.currency} ${account.accountId} " +
                        "${firstDate}_${lastDate}"
                    val saved = inIntegrationTransaction(integrationId = integrationId) { current ->
                        if (standaloneDocuments.existsByWorkspaceIdAndTitle(workspaceId = current.workspaceId, title = title)) return@inIntegrationTransaction false
                        client.getStatementPdf(
                            settings = current, profileId = account.profileId, accountId = account.accountId, currency = account.currency,
                            start = start, end = end,
                        ) { content ->
                            val document = documents.saveDocument(SaveDocumentRequest(
                                fileName = "$title.pdf", content = inputStreamProvider { content }, workspace = workspace, contentType = "application/pdf",
                            ))
                            standaloneDocuments.save(StandaloneDocument(title = title, documentId = document.id!!))
                        }
                        true
                    }
                    outcome = when (saved) {
                        true -> "CREATED"
                        false -> "EXISTING"
                        null -> "MISSING"
                    }
                    if (saved == true) logger.info {
                        "Wise monthly statement document committed: $context, month=$month"
                    }
                } finally {
                    logger.info { "Wise monthly statement download finished: $context, outcome=$outcome, elapsedMs=${elapsedMillis(started)}" }
                }
            }
        }
    }

    private fun elapsedMillis(started: Long): Long = (System.nanoTime() - started) / 1_000_000

    private data class FetchResult(
        val outcome: FetchOutcome,
        val fetched: Int = 0,
        val imported: Int = 0,
        val transactionsFetched: Boolean = false,
    )

    private enum class FetchOutcome { SUCCESS, MISSING, DISABLED, AUTHENTICATION_REQUIRED, NO_ACCOUNTS, PROVIDER_FAILURE, FAILED }
}
