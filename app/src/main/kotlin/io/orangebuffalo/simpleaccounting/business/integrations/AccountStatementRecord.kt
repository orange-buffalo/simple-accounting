package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import java.math.BigDecimal
import java.time.Instant

@Table
data class AccountStatementRecord(
    val workspaceId: String,
    val provider: IntegrationProvider,
    val integrationId: String,
    val accountId: String,
    val accountCurrency: String,
    val externalId: String,
    val type: StatementType,
    val occurredAt: Instant,
    /** Absolute transaction amount in the currency's minor units; never negative. */
    val amountInCents: Long,
    val currency: String,
    val partyName: String? = null,
    val feesInCents: Long? = null,
    val feesCurrency: String? = null,
    val runningBalanceInCents: Long? = null,
    val runningBalanceCurrency: String? = null,
    val additionalData: String? = null,
    val originalAmountInCents: Long? = null,
    val originalCurrency: String? = null,
    val sourceAmountInCents: Long? = null,
    val sourceCurrency: String? = null,
    val targetAmountInCents: Long? = null,
    val targetCurrency: String? = null,
    val exchangeRate: BigDecimal? = null,
    val linkedInvoiceId: String? = null,
    val linkedIncomeId: String? = null,
    val title: String? = null,
    @field:MappedCollection(idColumn = "account_statement_id")
    val attachments: Set<AccountStatementAttachment> = emptySet(),
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity() {
    init {
        require(amountInCents >= 0) { "Statement amounts must not be negative" }
    }
}

enum class IntegrationProvider { WISE }

@Table("account_statement_attachments")
data class AccountStatementAttachment(val documentId: String)

enum class StatementType { DEBIT, CREDIT, CONVERSION, OTHER }

interface AccountStatementRecordsRepository : AbstractEntityRepository<AccountStatementRecord> {
    @Query("""
        select r.* from account_statement_record r
        where r.workspace_id = :workspaceId and r.integration_id = :integrationId
          and r.type = 'CREDIT' and r.linked_income_id is null and r.linked_invoice_id is not null
          and not exists (select 1 from income i where i.linked_invoice_id = r.linked_invoice_id and i.workspace_id = r.workspace_id)
          and not exists (select 1 from invoice i join customer c on c.id = i.customer_id
                          where i.id = r.linked_invoice_id and c.workspace_id = r.workspace_id
                           and (i.date_paid is not null or i.status in ('PAID', 'CANCELLED')))
        order by r.occurred_at, r.id
    """)
    fun findForIncomeCreation(workspaceId: String, integrationId: String): List<AccountStatementRecord>

    @Query("""
        select r.* from account_statement_record r
        where r.workspace_id = :workspaceId and r.integration_id = :integrationId
          and r.type = 'CONVERSION' and r.linked_income_id is null
          and r.source_amount_in_cents > 0 and r.target_amount_in_cents >= 0
          and r.source_currency is not null and r.target_currency = :defaultCurrency
        order by r.occurred_at, r.id
    """)
    fun findForIncomeConversion(workspaceId: String, integrationId: String, defaultCurrency: String): List<AccountStatementRecord>

    @Query("""
        select r.* from account_statement_record r
        where r.workspace_id = :workspaceId and r.provider = :provider
          and r.integration_id = :integrationId
          and not exists (select 1 from account_statement_attachments a where a.account_statement_id = r.id)
        order by r.occurred_at, r.id
    """)
    fun findWithoutDocuments(workspaceId: String, provider: String, integrationId: String): List<AccountStatementRecord>

    fun findByWorkspaceIdAndLinkedInvoiceIdIsNullAndType(workspaceId: String, type: StatementType): List<AccountStatementRecord>

    fun findByWorkspaceIdAndIntegrationIdAndLinkedIncomeIdIsNullAndType(
        workspaceId: String, integrationId: String, type: StatementType,
    ): List<AccountStatementRecord>

    @Modifying
    @Query("delete from account_statement_record where integration_id = :integrationId and provider = :provider")
    fun deleteByIntegrationIdAndProvider(integrationId: String, provider: String)

    fun findByIntegrationIdAndAccountIdAndExternalId(
        integrationId: String,
        accountId: String,
        externalId: String,
    ): AccountStatementRecord?
}
