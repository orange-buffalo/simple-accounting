package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant

@Table
data class AccountStatementRecord(
    val workspaceId: String,
    val provider: String,
    val integrationId: String,
    val profileId: String,
    val accountId: String,
    val accountCurrency: String,
    val externalId: String,
    val direction: StatementDirection,
    val occurredAt: Instant,
    val amountInCents: Long,
    val currency: String,
    val feesInCents: Long? = null,
    val feesCurrency: String? = null,
    val runningBalanceInCents: Long? = null,
    val runningBalanceCurrency: String? = null,
    val transactionType: String? = null,
    val description: String? = null,
    val paymentReference: String? = null,
    val counterpartyName: String? = null,
    val counterpartyAccount: String? = null,
    val merchantCategory: String? = null,
    val merchantAddress: String? = null,
    val originalAmountInCents: Long? = null,
    val originalCurrency: String? = null,
    val sourceAmountInCents: Long? = null,
    val sourceCurrency: String? = null,
    val targetAmountInCents: Long? = null,
    val targetCurrency: String? = null,
    val exchangeRate: BigDecimal? = null,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

enum class StatementDirection { DEBIT, CREDIT }

interface AccountStatementRecordsRepository : AbstractEntityRepository<AccountStatementRecord> {
    fun findByProviderAndIntegrationIdAndProfileIdAndAccountIdAndAccountCurrencyAndExternalIdAndDirection(
        provider: String,
        integrationId: String,
        profileId: String,
        accountId: String,
        accountCurrency: String,
        externalId: String,
        direction: StatementDirection,
    ): AccountStatementRecord?
}
