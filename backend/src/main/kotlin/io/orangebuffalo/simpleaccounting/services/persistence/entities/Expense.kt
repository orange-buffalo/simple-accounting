package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
class Expense(

    @field:ManyToOne
    @field:JoinColumn(foreignKey = ForeignKey(name = "expense_category_fk"))
    var category: Category?,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "expense_workspace_fk"))
    var workspace: Workspace,

    @field:Column(nullable = false)
    var title: String,

    @field:Column(nullable = false)
    var timeRecorded: Instant,

    @field:Column(nullable = false)
    var datePaid: LocalDate,

    /**
     * Original currency of this receipt/invoice/etc.
     */
    @field:Column(nullable = false, length = 3)
    var currency: String,

    /**
     * Amount in original currency as stated in the receipt/invoice/etc.
     */
    @field:Column(nullable = false)
    var originalAmount: Long,

    /**
     * Converted amounts in default currency (i.e. from bank transaction in domestic currency).
     */
    @field:Embedded
    @field:AttributeOverrides(
        AttributeOverride(
            name = "originalAmountInDefaultCurrency",
            column = Column(name = "converted_original_amount_in_default_currency")
        ),
        AttributeOverride(
            name = "adjustedAmountInDefaultCurrency",
            column = Column(name = "converted_adjusted_amount_in_default_currency")
        )
    )
    var convertedAmounts: AmountsInDefaultCurrency,

    /**
     * Indicates if [incomeTaxableAmounts] are using different exchange rate than [convertedAmounts]
     * (i.e. by using Tax Office exchange rate).
     */
    @field:Column(nullable = false)
    var useDifferentExchangeRateForIncomeTaxPurposes: Boolean,

    /**
     * Amounts for income tax purposes. In case [useDifferentExchangeRateForIncomeTaxPurposes]
     * is `false`, are the same as [convertedAmounts]. Otherwise amounts are different.
     */
    @field:Column(nullable = false)
    @field:AttributeOverrides(
        AttributeOverride(
            name = "originalAmountInDefaultCurrency",
            column = Column(name = "income_taxable_original_amount_in_default_currency")
        ),
        AttributeOverride(
            name = "adjustedAmountInDefaultCurrency",
            column = Column(name = "income_taxable_adjusted_amount_in_default_currency")
        )
    )
    var incomeTaxableAmounts: AmountsInDefaultCurrency,

    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "expense_attachments",
        foreignKey = ForeignKey(name = "expense_attachments_expense_fk"),
        inverseForeignKey = ForeignKey(name = "expense_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var attachments: Set<Document> = setOf(),

    @field:Column(nullable = false)
    var percentOnBusiness: Int,

    @field:ManyToOne
    @field:JoinColumn(foreignKey = ForeignKey(name = "expense_general_tax_fk"))
    var generalTax: GeneralTax?,

    @field:Column
    var generalTaxRateInBps: Int? = null,

    /**
     * Amount that is a part of original amount (after conversion to the default currency,
     * if applicable) and is related to the [GeneralTax] applied to this expense.
     * This amount is deducted from the `originalAmountInDefaultCurrency` when
     * `adjustedAmountInDefaultCurrency` is calculated.
     */
    @field:Column
    var generalTaxAmount: Long? = null,

    @field:Column(length = 1024)
    var notes: String? = null,

    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    var status: ExpenseStatus

) : AbstractEntity() {

    init {
        require(category == null || category?.workspace == workspace) { "Category and workspace must match" }

        require(generalTax == null || generalTax?.workspace == workspace) { "Tax and workspace must match" }
    }
}

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
