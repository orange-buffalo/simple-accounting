package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table
data class Invoice(
    val customerId: String,
    val title: String,
    val dateIssued: LocalDate,
    val dateSent: LocalDate? = null,
    val datePaid: LocalDate? = null,
    val timeCancelled: Instant? = null,
    val dueDate: LocalDate,
    val currency: String,
    val amount: Long,
    @field:MappedCollection(idColumn = "INVOICE_ID")
    val attachments: Set<InvoiceAttachment> = setOf(),
    val notes: String? = null,
    val generalTaxId: String? = null,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,

) : AbstractEntity()

@Table("INVOICE_ATTACHMENTS")
data class InvoiceAttachment(
    val documentId: String
)

enum class InvoiceStatus {
    DRAFT,
    SENT,
    OVERDUE,
    PAID,
    CANCELLED
}
