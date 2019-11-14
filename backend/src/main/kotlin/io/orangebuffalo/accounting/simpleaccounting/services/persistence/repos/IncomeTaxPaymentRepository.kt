package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import com.querydsl.core.annotations.QueryProjection
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.IncomeTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import java.time.LocalDate

interface IncomeTaxPaymentRepository :
    AbstractEntityRepository<IncomeTaxPayment>, QuerydslPredicateExecutor<IncomeTaxPayment>,
    IncomeTaxPaymentRepositoryExt {

    fun findByIdAndWorkspace(id: Long, workspace: Workspace): IncomeTaxPayment?
}

interface IncomeTaxPaymentRepositoryExt {
    fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): IncomeTaxPaymentsStatistics
}

data class IncomeTaxPaymentsStatistics @QueryProjection constructor(
    val totalTaxPayments: Long
)
