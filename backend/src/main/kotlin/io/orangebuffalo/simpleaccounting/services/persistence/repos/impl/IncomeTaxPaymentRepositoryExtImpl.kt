package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.fetchExactlyOne
import io.orangebuffalo.simpleaccounting.services.persistence.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeTaxPaymentRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeTaxPaymentsStatistics
import org.jooq.DSLContext
import org.jooq.impl.DSL.coalesce
import org.jooq.impl.DSL.sum
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class IncomeTaxPaymentRepositoryExtImpl(
    private val dslContext: DSLContext
) : IncomeTaxPaymentRepositoryExt {

    private val taxPayment = Tables.INCOME_TAX_PAYMENT

    override fun getTaxPaymentsStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): IncomeTaxPaymentsStatistics = dslContext
        .select(coalesce(sum(taxPayment.amount), 0L).mapTo(IncomeTaxPaymentsStatistics::totalTaxPayments))
        .from(taxPayment)
        .where(
            taxPayment.workspaceId.eq(workspaceId),
            taxPayment.reportingDate.greaterOrEqual(fromDate),
            taxPayment.reportingDate.lessOrEqual(toDate)
        )
        .fetchExactlyOne()
}
