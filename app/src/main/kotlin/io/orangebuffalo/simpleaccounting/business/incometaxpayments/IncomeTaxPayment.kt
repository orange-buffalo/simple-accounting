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

    @field:MappedCollection(idColumn = "INCOME_TAX_PAYMENT_ID")
    val attachments: Set<IncomeTaxPaymentAttachment> = setOf(),

    val notes: String? = null,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,

) : AbstractEntity()

@Table("INCOME_TAX_PAYMENT_ATTACHMENTS")
data class IncomeTaxPaymentAttachment(
    val documentId: String
)
