package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import java.time.LocalDate

interface IncomeTaxPaymentsRepository : AbstractEntityRepository<IncomeTaxPayment>, IncomeTaxPaymentsRepositoryExt {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): IncomeTaxPayment?
}

interface IncomeTaxPaymentsRepositoryExt {
    fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: String
    ): IncomeTaxPaymentsStatistics
}

data class IncomeTaxPaymentsStatistics(
    val totalTaxPayments: Long
)
