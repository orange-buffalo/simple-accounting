package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import com.querydsl.core.annotations.QueryProjection
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.TaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import java.time.LocalDate

interface TaxPaymentRepository :
    AbstractEntityRepository<TaxPayment>, QuerydslPredicateExecutor<TaxPayment>, TaxPaymentRepositoryExt {
    fun findByIdAndWorkspace(id: Long, workspace: Workspace): TaxPayment?
}

interface TaxPaymentRepositoryExt {
    fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): TaxPaymentsStatistics
}

data class TaxPaymentsStatistics @QueryProjection constructor(
    val totalTaxPayments: Long
)