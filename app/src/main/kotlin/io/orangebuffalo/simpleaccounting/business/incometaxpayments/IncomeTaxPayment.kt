package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table
data class IncomeTaxPayment(

    val workspaceId: String,
    val datePaid: LocalDate,
    val reportingDate: LocalDate,
    val amount: Long,
    val title: String,

    @field:MappedCollection(idColumn = "income_tax_payment_id")
    val attachments: Set<IncomeTaxPaymentAttachment> = setOf(),

    val notes: String? = null,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,

) : AbstractEntity()

@Table("income_tax_payment_attachments")
data class IncomeTaxPaymentAttachment(
    val documentId: String
)
