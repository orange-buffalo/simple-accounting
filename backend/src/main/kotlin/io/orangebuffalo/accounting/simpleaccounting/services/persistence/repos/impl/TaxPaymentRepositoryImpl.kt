package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.impl

import com.querydsl.jpa.impl.JPAQuery
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QIncomeTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeTaxPaymentRepositoryExt
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeTaxPaymentsStatistics
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.QIncomeTaxPaymentsStatistics
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.persistence.EntityManager

@Component
class IncomeTaxPaymentRepositoryExtImpl(
    private val entityManager: EntityManager
) : IncomeTaxPaymentRepositoryExt {
    override fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): IncomeTaxPaymentsStatistics {
        val taxPayment = QIncomeTaxPayment.incomeTaxPayment
        return JPAQuery<IncomeTaxPaymentsStatistics>(entityManager)
            .from(taxPayment)
            .where(
                taxPayment.workspace.eq(workspace),
                taxPayment.reportingDate.goe(fromDate),
                taxPayment.reportingDate.loe(toDate)
            )
            .select(
                QIncomeTaxPaymentsStatistics(
                    taxPayment.amount.sum().coalesce(0)
                )
            )
            .fetchFirst()
    }
}
