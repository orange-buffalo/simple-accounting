package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table
class Invoice(
    var customerId: Long,
    var title: String,
    var dateIssued: LocalDate,
    var dateSent: LocalDate? = null,
    var datePaid: LocalDate? = null,
    var timeCancelled: Instant? = null,
    var dueDate: LocalDate,
    var currency: String,
    var amount: Long,
    @field:MappedCollection(idColumn = "INVOICE_ID")
    var attachments: Set<InvoiceAttachment> = setOf(),
    var notes: String? = null,
    var generalTaxId: Long? = null,
    var status: InvoiceStatus = InvoiceStatus.DRAFT

) : AbstractEntity()

@Table("INVOICE_ATTACHMENTS")
data class InvoiceAttachment(
    val documentId: Long
)

enum class InvoiceStatus {
    DRAFT,
    SENT,
    OVERDUE,
    PAID,
    CANCELLED
}
