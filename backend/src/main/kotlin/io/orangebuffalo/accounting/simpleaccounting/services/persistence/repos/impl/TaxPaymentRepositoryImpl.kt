package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.impl

import com.querydsl.jpa.impl.JPAQuery
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QTaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.QTaxPaymentsStatistics
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.TaxPaymentRepositoryExt
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.TaxPaymentsStatistics
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.persistence.EntityManager

@Component
class TaxPaymentRepositoryExtImpl(
    private val entityManager: EntityManager
) : TaxPaymentRepositoryExt {
    override fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspace: Workspace
    ): TaxPaymentsStatistics {
        val taxPayment = QTaxPayment.taxPayment
        return JPAQuery<TaxPaymentsStatistics>(entityManager)
            .from(taxPayment)
            .where(
                taxPayment.workspace.eq(workspace),
                taxPayment.datePaid.goe(fromDate),
                taxPayment.datePaid.loe(toDate)
            )
            .select(
                QTaxPaymentsStatistics(
                    taxPayment.amount.sum()
                )
            )
            .fetchFirst()
    }
}