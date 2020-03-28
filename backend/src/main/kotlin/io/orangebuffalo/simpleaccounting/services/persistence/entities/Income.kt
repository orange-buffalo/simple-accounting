package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
class Income(

    @field:ManyToOne
    @field:JoinColumn(foreignKey = ForeignKey(name = "income_category_fk"))
    var category: Category?,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "income_workspace_fk"))
    var workspace: Workspace,

    @field:Column(nullable = false)
    var title: String,

    @field:Column(nullable = false)
    var timeRecorded: Instant,

    @field:Column(nullable = false)
    var dateReceived: LocalDate,

    /**
     * Original currency of this income.
     */
    @field:Column(nullable = false, length = 3)
    var currency: String,

    /**
     * Amount in original currency.
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
        name = "income_attachments",
        foreignKey = ForeignKey(name = "income_attachments_income_fk"),
        inverseForeignKey = ForeignKey(name = "income_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var attachments: Set<Document> = setOf(),

    @field:Column(length = 1024)
    var notes: String? = null,

    @field:ManyToOne
    @field:JoinColumn(foreignKey = ForeignKey(name = "income_general_tax_fk"))
    var generalTax: GeneralTax?,

    @field:Column
    var generalTaxRateInBps: Int? = null,

    @field:Column
    var generalTaxAmount: Long? = null,

    @field:Column(nullable = false)
    @field:Enumerated(EnumType.STRING)
    var status: IncomeStatus

) : AbstractEntity() {

    init {
        require(category == null || category?.workspace == workspace) { "Category and workspace must match" }

        require(generalTax == null || generalTax?.workspace == workspace) { "Tax and workspace must match" }
    }
}

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
