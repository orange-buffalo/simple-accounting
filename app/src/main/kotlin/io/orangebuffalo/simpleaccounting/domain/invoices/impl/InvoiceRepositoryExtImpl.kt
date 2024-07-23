package io.orangebuffalo.simpleaccounting.domain.invoices.impl

import io.orangebuffalo.simpleaccounting.domain.invoices.Invoice
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.domain.invoices.InvoiceRepositoryExt
import io.orangebuffalo.simpleaccounting.domain.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class InvoiceRepositoryExtImpl(
    private val dslContext: DSLContext,
    private val timeService: TimeService
) : InvoiceRepositoryExt {
    private val invoice = Tables.INVOICE
    private val customer = Tables.CUSTOMER

    override fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Invoice? = dslContext
        .select(*invoice.fields())
        .from(invoice)
        .join(customer).on(invoice.customerId.eq(customer.id))
        .where(
            invoice.id.eq(id),
            customer.workspaceId.eq(workspaceId)
        )
        .fetchOneOrNull()

    override fun findAllOverdue(): List<Invoice> = dslContext
        .select(*invoice.fields())
        .from(invoice)
        .where(
            invoice.status.eq(InvoiceStatus.SENT),
            invoice.dueDate.lessThan(timeService.currentDate())
        )
        .fetchListOf()
}
