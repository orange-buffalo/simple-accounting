package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeTaxPayment
import java.time.LocalDate

interface IncomeTaxPaymentRepository : AbstractEntityRepository<IncomeTaxPayment>, IncomeTaxPaymentRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): IncomeTaxPayment?
}

interface IncomeTaxPaymentRepositoryExt {
    fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): IncomeTaxPaymentsStatistics
}

data class IncomeTaxPaymentsStatistics(
    val totalTaxPayments: Long
)
