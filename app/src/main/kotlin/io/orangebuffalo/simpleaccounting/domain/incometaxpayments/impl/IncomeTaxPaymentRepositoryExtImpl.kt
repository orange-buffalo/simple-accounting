package io.orangebuffalo.simpleaccounting.domain.incometaxpayments.impl

import io.orangebuffalo.simpleaccounting.infra.jooq.fetchExactlyOne
import io.orangebuffalo.simpleaccounting.infra.jooq.mapTo
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.domain.incometaxpayments.IncomeTaxPaymentRepositoryExt
import io.orangebuffalo.simpleaccounting.domain.incometaxpayments.IncomeTaxPaymentsStatistics
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
