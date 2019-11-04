package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.IncomeTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QIncomeTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeTaxPaymentRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeTaxPaymentsStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomeTaxPaymentService(
    private val taxPaymentRepository: IncomeTaxPaymentRepository
) {

    suspend fun saveTaxPayment(taxPayment: IncomeTaxPayment): IncomeTaxPayment {
        return withDbContext {
            taxPaymentRepository.save(taxPayment)
        }
    }

    suspend fun getTaxPayments(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<IncomeTaxPayment> = withDbContext {
        taxPaymentRepository.findAll(QIncomeTaxPayment.incomeTaxPayment.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getTaxPaymentByIdAndWorkspace(id: Long, workspace: Workspace): IncomeTaxPayment? =
        withDbContext {
            taxPaymentRepository.findByIdAndWorkspace(id, workspace)
        }

    suspend fun getTaxPaymentStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): IncomeTaxPaymentsStatistics = withDbContext {
        taxPaymentRepository.getTaxPaymentsStatistics(fromDate, toDate, workspace)
    }
}
