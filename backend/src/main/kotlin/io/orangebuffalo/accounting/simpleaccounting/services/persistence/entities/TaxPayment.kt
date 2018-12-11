package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
class TaxPayment(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "tax_payment_workspace_fk"))
    var workspace: Workspace,

    @field:Column(nullable = false)
    val timeRecorded: Instant,

    @field:Column(nullable = false)
    var datePaid: LocalDate,

    @field:Column(nullable = false)
    var amount: Long,

    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "tax_payment_attachments",
        foreignKey = ForeignKey(name = "tax_payment_attachments_tax_payment_fk"),
        inverseForeignKey = ForeignKey(name = "tax_payment_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var attachments: Set<Document> = setOf(),

    @field:Column(length = 1024)
    var notes: String? = null

) : AbstractEntity()