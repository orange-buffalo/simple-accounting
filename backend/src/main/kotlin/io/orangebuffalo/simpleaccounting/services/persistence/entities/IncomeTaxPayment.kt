package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
class IncomeTaxPayment(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "income_tax_payment_workspace_fk"))
    var workspace: Workspace,

    @field:Column(nullable = false)
    var timeRecorded: Instant,

    @field:Column(nullable = false)
    var datePaid: LocalDate,

    @field:Column(nullable = false)
    var reportingDate: LocalDate,

    @field:Column(nullable = false)
    var amount: Long,

    @field:Column(nullable = false, length = 255)
    var title: String,

    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "income_tax_payment_attachments",
        foreignKey = ForeignKey(name = "income_tax_payment_attachments_tax_payment_fk"),
        inverseForeignKey = ForeignKey(name = "income_tax_payment_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var attachments: Set<Document> = setOf(),

    @field:Column(length = 1024)
    var notes: String? = null

) : AbstractEntity()
