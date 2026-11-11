package io.orangebuffalo.simpleaccounting.business.api.accountstatements

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.incomes.IncomeGqlDto
import io.orangebuffalo.simpleaccounting.business.api.incomes.loadIncomeByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.invoices.InvoiceGqlDto
import io.orangebuffalo.simpleaccounting.business.api.invoices.loadInvoiceByWorkspaceAndId
import io.orangebuffalo.simpleaccounting.business.api.documents.DocumentGqlDto
import io.orangebuffalo.simpleaccounting.business.api.documents.loadDocumentsByIds
import io.orangebuffalo.simpleaccounting.business.integrations.IntegrationProvider
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.GraphqlPaginationService
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.impl.DSL
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Component
class AccountStatementsGqlApi(
    private val paginationService: GraphqlPaginationService,
    private val dslContext: DSLContext,
) {
    private val statement = Tables.ACCOUNT_STATEMENT_RECORD
    private val statementAttachments = Tables.ACCOUNT_STATEMENT_ATTACHMENTS

    fun loadStatements(
        workspaceId: String,
        first: Int,
        after: String?,
        freeSearchText: String?,
        providerIn: List<IntegrationProvider>?,
        occurredAtFrom: Instant?,
        occurredAtBefore: Instant?,
    ): ConnectionGqlDto<AccountStatementGqlDto> = paginationService.forTable(statement)
        .addPredicate(statement.workspaceId.eq(workspaceId))
        .also { query ->
            if (!freeSearchText.isNullOrBlank()) {
                query.addPredicate(DSL.or(listOf(
                    statement.accountId, statement.externalId, statement.type,
                    statement.partyName, statement.additionalData,
                ).map { it.containsIgnoreCase(freeSearchText) }))
            }
            if (!providerIn.isNullOrEmpty()) query.addPredicate(statement.provider.`in`(providerIn.map { it.name }))
            if (occurredAtFrom != null) query.addPredicate(statement.occurredAt.ge(occurredAtFrom))
            if (occurredAtBefore != null) query.addPredicate(statement.occurredAt.lt(occurredAtBefore))
        }
        .page(first = first, after = after, sortFields = listOf(statement.occurredAt.desc(), statement.id.desc()), mapQueryRecord = { record ->
            AccountStatementGqlDto(
                id = record[statement.id]!!,
                provider = IntegrationProvider.valueOf(record[statement.provider]!!),
                integrationId = record[statement.integrationId]!!,
                accountId = record[statement.accountId]!!,
                accountCurrency = record[statement.accountCurrency]!!,
                externalId = record[statement.externalId]!!,
                type = StatementType.valueOf(record[statement.type]!!),
                occurredAt = record[statement.occurredAt]!!,
                amountInCents = record[statement.amountInCents]!!,
                currency = record[statement.currency]!!,
                partyName = record[statement.partyName],
                feesInCents = record[statement.feesInCents],
                feesCurrency = record[statement.feesCurrency],
                runningBalanceInCents = record[statement.runningBalanceInCents],
                runningBalanceCurrency = record[statement.runningBalanceCurrency],
                additionalData = record[statement.additionalData],
                originalAmountInCents = record[statement.originalAmountInCents],
                originalCurrency = record[statement.originalCurrency],
                sourceAmountInCents = record[statement.sourceAmountInCents],
                sourceCurrency = record[statement.sourceCurrency],
                targetAmountInCents = record[statement.targetAmountInCents],
                targetCurrency = record[statement.targetCurrency],
                exchangeRate = record[statement.exchangeRate]?.toPlainString(),
                createdAt = record[statement.createdAt]!!,
                workspaceId = workspaceId,
                linkedInvoiceId = record[statement.linkedInvoiceId],
                linkedIncomeId = record[statement.linkedIncomeId],
                title = record[statement.title],
                attachmentIds = emptyList(),
            )
        }, postProcess = { records ->
            val attachmentsByStatementId = dslContext
                .select(statementAttachments.accountStatementId, statementAttachments.documentId)
                .from(statementAttachments)
                .where(statementAttachments.accountStatementId.`in`(records.map { it.id }))
                .orderBy(statementAttachments.documentId)
                .fetch()
                .groupBy(
                    keySelector = { it[statementAttachments.accountStatementId]!! },
                    valueTransform = { it[statementAttachments.documentId]!! },
                )
            records.map { dto -> dto.copy(attachmentIds = attachmentsByStatementId[dto.id].orEmpty()) }
        })
}

@GraphQLName("AccountStatement")
@GraphQLDescription("An imported account statement transaction. Provider identifiers are opaque.")
data class AccountStatementGqlDto(
    @GraphQLDescription("Internal statement record ID.") val id: String,
    @GraphQLDescription("Source integration provider.") val provider: IntegrationProvider,
    @GraphQLDescription("Source integration ID.") val integrationId: String,
    @GraphQLDescription("Opaque provider account ID.") val accountId: String,
    @GraphQLDescription("Currency of the source account.") val accountCurrency: String,
    @GraphQLDescription("Opaque external transaction ID.") val externalId: String,
    @GraphQLDescription("Transaction classification.") val type: StatementType,
    @GraphQLDescription("Time of the transaction.") val occurredAt: Instant,
    @GraphQLDescription("Absolute nonnegative transaction amount in currency minor units.") val amountInCents: Long,
    @GraphQLDescription("Transaction currency.") val currency: String,
    @GraphQLDescription("Other party's name, when supplied by the provider.") val partyName: String?,
    @GraphQLDescription("Provider's descriptive transaction title, when available.") val title: String?,
    @GraphQLDescription("Fees in minor units.") val feesInCents: Long?,
    @GraphQLDescription("Fee currency.") val feesCurrency: String?,
    @GraphQLDescription("Signed running balance in minor units.") val runningBalanceInCents: Long?,
    @GraphQLDescription("Running balance currency.") val runningBalanceCurrency: String?,
    @GraphQLDescription("Additional labeled transaction information supplied by the provider.") val additionalData: String?,
    @GraphQLDescription("Original amount in minor units.") val originalAmountInCents: Long?,
    @GraphQLDescription("Original currency.") val originalCurrency: String?,
    @GraphQLDescription("Conversion source amount in minor units.") val sourceAmountInCents: Long?,
    @GraphQLDescription("Conversion source currency.") val sourceCurrency: String?,
    @GraphQLDescription("Conversion target amount in minor units.") val targetAmountInCents: Long?,
    @GraphQLDescription("Conversion target currency.") val targetCurrency: String?,
    @GraphQLDescription("Exact decimal provider exchange rate, represented as a string.") val exchangeRate: String?,
    @GraphQLDescription("Time this record was imported.") val createdAt: Instant,
    @GraphQLIgnore val workspaceId: String,
    @GraphQLIgnore val linkedInvoiceId: String?,
    @GraphQLIgnore val linkedIncomeId: String?,
    @GraphQLIgnore val attachmentIds: List<String>,
) {
    @GraphQLDescription("Documents attached to this statement.")
    fun attachments(env: DataFetchingEnvironment): CompletableFuture<List<DocumentGqlDto>> {
        if (attachmentIds.isEmpty()) return CompletableFuture.completedFuture(emptyList())
        return env.loadDocumentsByIds(attachmentIds)
    }

    @GraphQLDescription("Invoice linked to this statement, if a unique match was found during synchronization.")
    fun linkedInvoice(env: DataFetchingEnvironment): CompletableFuture<InvoiceGqlDto?>? {
        val invoiceId = linkedInvoiceId ?: return null
        return env.loadInvoiceByWorkspaceAndId(workspaceId = workspaceId, invoiceId = invoiceId)
    }

    @GraphQLDescription("Income linked to this statement during synchronization.")
    fun linkedIncome(env: DataFetchingEnvironment): CompletableFuture<IncomeGqlDto?>? {
        val incomeId = linkedIncomeId ?: return null
        return env.loadIncomeByWorkspaceAndId(workspaceId = workspaceId, incomeId = incomeId)
    }
}
