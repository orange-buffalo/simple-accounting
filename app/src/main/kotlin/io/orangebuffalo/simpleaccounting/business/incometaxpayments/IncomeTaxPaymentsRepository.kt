package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository
import java.time.LocalDate

interface IncomeTaxPaymentsRepository : AbstractEntityRepository<IncomeTaxPayment>, IncomeTaxPaymentsRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): IncomeTaxPayment?
}

interface IncomeTaxPaymentsRepositoryExt {
    fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): IncomeTaxPaymentsStatistics
}

data class IncomeTaxPaymentsStatistics(
    val totalTaxPayments: Long
)
