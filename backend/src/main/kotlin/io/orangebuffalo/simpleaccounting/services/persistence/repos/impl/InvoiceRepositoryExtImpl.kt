package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.InvoiceRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class InvoiceRepositoryExtImpl(
    private val dslContext: DSLContext
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
}
