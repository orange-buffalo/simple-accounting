package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table
data class Income(
    val categoryId: String?,
    val workspaceId: String,
    val title: String,
    val dateReceived: LocalDate,

    /**
     * Original currency of this income.
     */
    val currency: String,

    /**
     * Amount in original currency.
     */
    val originalAmount: Long,

    /**
     * Converted amounts in default currency (i.e. from bank transaction in domestic currency).
     */
    @field:Embedded.Empty(prefix = "CONVERTED_")
    val convertedAmounts: AmountsInDefaultCurrency,

    /**
     * Indicates if [incomeTaxableAmounts] are using different exchange rate than [convertedAmounts]
     * (i.e. by using Tax Office exchange rate).
     */
    val useDifferentExchangeRateForIncomeTaxPurposes: Boolean,

    /**
     * Amounts for income tax purposes. In case [useDifferentExchangeRateForIncomeTaxPurposes]
     * is `false`, are the same as [convertedAmounts]. Otherwise amounts are different.
     */
    @field:Embedded.Empty(prefix = "INCOME_TAXABLE_")
    val incomeTaxableAmounts: AmountsInDefaultCurrency,

    @field:MappedCollection(idColumn = "INCOME_ID")
    val attachments: Set<IncomeAttachment> = setOf(),

    val notes: String? = null,

    val generalTaxId: String?,

    val generalTaxRateInBps: Int? = null,

    val generalTaxAmount: Long? = null,

    val status: IncomeStatus,

    val linkedInvoiceId: String? = null,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,

) : AbstractEntity()

@Table("INCOME_ATTACHMENTS")
data class IncomeAttachment(
    val documentId: String
)

enum class IncomeStatus {

    /**
     * All data has been provided, all amounts calculated.
     */
    FINALIZED,

    /**
     * At least [Income.convertedAmounts] has not yet been provided.
     */
    PENDING_CONVERSION,

    /**
     * [Income.useDifferentExchangeRateForIncomeTaxPurposes] is set to `true`
     * and [Income.incomeTaxableAmounts] has not been provided yet.
     */
    PENDING_CONVERSION_FOR_TAXATION_PURPOSES
}
