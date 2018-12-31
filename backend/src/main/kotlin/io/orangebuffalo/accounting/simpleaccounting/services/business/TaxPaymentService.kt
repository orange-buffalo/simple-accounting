package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.TaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.TaxPaymentRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.TaxPaymentsStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TaxPaymentService(
    private val taxPaymentRepository: TaxPaymentRepository
) {

    suspend fun saveTaxPayment(taxPayment: TaxPayment): TaxPayment {
        return withDbContext {
            taxPaymentRepository.save(taxPayment)
        }
    }

    suspend fun getTaxPayments(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<TaxPayment> = withDbContext {
        taxPaymentRepository.findAll(QTaxPayment.taxPayment.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getTaxPaymentByIdAndWorkspace(id: Long, workspace: Workspace): TaxPayment? =
        withDbContext {
            taxPaymentRepository.findByIdAndWorkspace(id, workspace)
        }

    suspend fun getTaxPaymentStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): TaxPaymentsStatistics = withDbContext {
        taxPaymentRepository.getTaxPaymentsStatistics(fromDate, toDate, workspace)
    }
}