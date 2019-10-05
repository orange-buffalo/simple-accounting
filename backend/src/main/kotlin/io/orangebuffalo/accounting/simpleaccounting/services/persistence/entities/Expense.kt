package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

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
    val timeRecorded: Instant,

    @field:Column(nullable = false)
    var datePaid: LocalDate,

    @field:Column(nullable = false, length = 3)
    var currency: String,

    /**
     * Amount in original currency as stated in the receipt/invoice/etc.
     */
    @field:Column(nullable = false)
    var originalAmount: Long,

    /**
     * Factual amount in default currency (i.e. from bank transaction in domestic currency).
     */
    @field:Column(nullable = false)
    var amountInDefaultCurrency: Long,

    /**
     * Amount converted to default currency for taxation purposed (i.e. by using Tax Office exchange rate).
     */
    @field:Column(nullable = false)
    var actualAmountInDefaultCurrency: Long,

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
    @field:JoinColumn(foreignKey = ForeignKey(name = "expense_tax_fk"))
    var tax: Tax?,

    @field:Column
    var taxRateInBps: Int? = null,

    @field:Column
    var taxAmount: Long? = null,

    /**
     * Amount to be reported for taxation purposes. Takes into account applicable tax,
     * partial business purpose of the expense and exchange rate used for conversion (if any).
     */
    @field:Column(nullable = false)
    var reportedAmountInDefaultCurrency: Long,

    @field:Column(length = 1024)
    var notes: String? = null

) : AbstractEntity() {

    init {
        require(!(category != null && category?.workspace != workspace)) { "Category and workspace must match" }

        require(!(tax != null && tax?.workspace != workspace)) { "Tax and workspace must match" }
    }
}