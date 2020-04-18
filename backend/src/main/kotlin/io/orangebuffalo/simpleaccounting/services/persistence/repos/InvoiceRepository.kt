package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface InvoiceRepository : LegacyAbstractEntityRepository<Invoice>, QuerydslPredicateExecutor<Invoice> {

    fun findByIdAndCustomerWorkspace(id: Long, workspace: Workspace): Invoice?

    fun findByIncomeId(income: Long): Invoice?
}
