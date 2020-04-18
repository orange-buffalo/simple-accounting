package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
class Invoice(

    @field:ManyToOne
    @field:JoinColumn(foreignKey = ForeignKey(name = "invoice_income_fk"))
    var income: LegacyIncome? = null,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = true, foreignKey = ForeignKey(name = "invoice_customer_fk"))
    var customer: Customer,

    @field:Column(nullable = false)
    var title: String,

    @field:Column(nullable = false)
    var timeRecorded: Instant,

    @field:Column(nullable = false)
    var dateIssued: LocalDate,

    @field:Column
    var dateSent: LocalDate? = null,

    @field:Column
    var datePaid: LocalDate? = null,

    @field:Column
    var dateCancelled: LocalDate? = null,

    @field:Column(nullable = false)
    var dueDate: LocalDate,

    @field:Column(nullable = false, length = 3)
    var currency: String,

    @field:Column(nullable = false)
    var amount: Long,

    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "invoice_attachments",
        foreignKey = ForeignKey(name = "invoice_attachments_invoice_fk"),
        inverseForeignKey = ForeignKey(name = "invoice_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var attachments: Set<Document> = setOf(),

    @field:Column(length = 1024)
    var notes: String? = null,

    @field:ManyToOne
    @field:JoinColumn(foreignKey = ForeignKey(name = "invoice_general_tax_fk"))
    var generalTax: GeneralTax? = null

) : LegacyAbstractEntity() {

    init {
        require(generalTax == null || generalTax?.workspace == customer.workspace) {
            "Tax and customer workspace must match"
        }
    }
}
