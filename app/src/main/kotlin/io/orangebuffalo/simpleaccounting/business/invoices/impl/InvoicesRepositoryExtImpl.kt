package io.orangebuffalo.simpleaccounting.business.invoices.impl

import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesRepositoryExt
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.ZoneId
import org.springframework.stereotype.Repository

@Repository
class InvoicesRepositoryExtImpl(
    private val dslContext: DSLContext,
    private val timeService: TimeService
) : InvoicesRepositoryExt {
    private val invoice = Tables.INVOICE
    private val workspace = Tables.WORKSPACE
    private val user = Tables.PLATFORM_USER

    override fun findByIdAndWorkspaceId(id: String, workspaceId: String): Invoice? = dslContext
        .select(*invoice.fields())
        .from(invoice)
        .where(
            invoice.id.eq(id),
            invoice.workspaceId.eq(workspaceId)
        )
        .fetchOneOrNull()

    override fun findAllOverdue(): List<Invoice> {
        val dueDateConditions = dslContext.selectDistinct(user.timeZone)
            .from(user)
            .fetch(user.timeZone)
            .map { timeZone ->
                val today = timeService.currentDate(ZoneId.of(timeZone))
                user.timeZone.eq(timeZone).and(invoice.dueDate.lessThan(today))
            }

        return dslContext
            .select(*invoice.fields())
            .from(invoice)
            .join(workspace).on(invoice.workspaceId.eq(workspace.id))
            .join(user).on(workspace.ownerId.eq(user.id))
            .where(invoice.status.eq(InvoiceStatus.SENT), DSL.or(dueDateConditions))
            .fetchListOf()
    }
}
