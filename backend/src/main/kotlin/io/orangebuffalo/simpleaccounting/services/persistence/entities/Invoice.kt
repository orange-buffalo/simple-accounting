package io.orangebuffalo.simpleaccounting.services.persistence.entities

import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table
class Invoice(

    var incomeId: Long? = null,
    var customerId: Long,
    var title: String,
    var timeRecorded: Instant,
    var dateIssued: LocalDate,
    var dateSent: LocalDate? = null,
    var datePaid: LocalDate? = null,
    var dateCancelled: LocalDate? = null,
    var dueDate: LocalDate,
    var currency: String,
    var amount: Long,
    @field:MappedCollection(idColumn = "INVOICE_ID")
    var attachments: Set<InvoiceAttachment> = setOf(),
    var notes: String? = null,
    var generalTaxId: Long? = null

) : AbstractEntity()

@Table("INVOICE_ATTACHMENTS")
data class InvoiceAttachment(
    val documentId: Long
)
