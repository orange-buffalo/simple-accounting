package io.orangebuffalo.simpleaccounting.business.incometaxpayments.impl

import io.orangebuffalo.simpleaccounting.infra.jooq.fetchExactlyOne
import io.orangebuffalo.simpleaccounting.infra.jooq.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentsRepositoryExt
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentsStatistics
import org.jooq.DSLContext
import org.jooq.impl.DSL.coalesce
import org.jooq.impl.DSL.sum
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class IncomeTaxPaymentsRepositoryExtImpl(
    private val dslContext: DSLContext
) : IncomeTaxPaymentsRepositoryExt {

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
