package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table
class Income(
    var categoryId: Long?,
    var workspaceId: Long,
    var title: String,
    var dateReceived: LocalDate,

    /**
     * Original currency of this income.
     */
    var currency: String,

    /**
     * Amount in original currency.
     */
    var originalAmount: Long,

    /**
     * Converted amounts in default currency (i.e. from bank transaction in domestic currency).
     */
    @field:Embedded.Empty(prefix = "CONVERTED_")
    var convertedAmounts: AmountsInDefaultCurrency,

    /**
     * Indicates if [incomeTaxableAmounts] are using different exchange rate than [convertedAmounts]
     * (i.e. by using Tax Office exchange rate).
     */
    var useDifferentExchangeRateForIncomeTaxPurposes: Boolean,

    /**
     * Amounts for income tax purposes. In case [useDifferentExchangeRateForIncomeTaxPurposes]
     * is `false`, are the same as [convertedAmounts]. Otherwise amounts are different.
     */
    @field:Embedded.Empty(prefix = "INCOME_TAXABLE_")
    var incomeTaxableAmounts: AmountsInDefaultCurrency,

    @field:MappedCollection(idColumn = "INCOME_ID")
    var attachments: Set<IncomeAttachment> = setOf(),

    var notes: String? = null,

    var generalTaxId: Long?,

    var generalTaxRateInBps: Int? = null,

    var generalTaxAmount: Long? = null,

    var status: IncomeStatus,

    var linkedInvoiceId: Long? = null

) : AbstractEntity()

@Table("INCOME_ATTACHMENTS")
data class IncomeAttachment(
    val documentId: Long
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
