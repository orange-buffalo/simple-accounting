package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table
class IncomeTaxPayment(

    var workspaceId: Long,
    var datePaid: LocalDate,
    var reportingDate: LocalDate,
    var amount: Long,
    var title: String,

    @field:MappedCollection(idColumn = "INCOME_TAX_PAYMENT_ID")
    var attachments: Set<IncomeTaxPaymentAttachment> = setOf(),

    var notes: String? = null

) : AbstractEntity()

@Table("INCOME_TAX_PAYMENT_ATTACHMENTS")
data class IncomeTaxPaymentAttachment(
    val documentId: Long
)
