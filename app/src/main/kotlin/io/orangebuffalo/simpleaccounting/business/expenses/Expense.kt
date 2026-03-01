package io.orangebuffalo.simpleaccounting.business.expenses

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import io.orangebuffalo.simpleaccounting.business.common.data.AmountsInDefaultCurrency
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table
class Expense(
    var categoryId: Long?,
    var workspaceId: Long,
    var title: String,
    var datePaid: LocalDate,

    /**
     * Original currency of this receipt/invoice/etc.
     */
    var currency: String,

    /**
     * Amount in original currency as stated in the receipt/invoice/etc.
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

    @field:MappedCollection(idColumn = "EXPENSE_ID")
    var attachments: Set<ExpenseAttachment> = setOf(),

    var percentOnBusiness: Int,

    var generalTaxId: Long?,

    var generalTaxRateInBps: Int? = null,

    /**
     * Amount that is a part of original amount (after conversion to the default currency,
     * if applicable) and is related to the [GeneralTax] applied to this expense.
     * This amount is deducted from the `originalAmountInDefaultCurrency` when
     * `adjustedAmountInDefaultCurrency` is calculated.
     */
    var generalTaxAmount: Long? = null,

    var notes: String? = null,

    var status: ExpenseStatus

) : AbstractEntity()

@Table("EXPENSE_ATTACHMENTS")
data class ExpenseAttachment(
    val documentId: Long
)

enum class ExpenseStatus {

    /**
     * All data has been provided, all amounts calculated.
     */
    FINALIZED,

    /**
     * At least [Expense.convertedAmounts] has not yet been provided.
     */
    PENDING_CONVERSION,

    /**
     * [Expense.useDifferentExchangeRateForIncomeTaxPurposes] is set to `true`
     * and [Expense.incomeTaxableAmounts] has not been provided yet.
     */
    PENDING_CONVERSION_FOR_TAXATION_PURPOSES
}
