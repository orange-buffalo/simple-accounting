package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface InvoiceRepository : AbstractEntityRepository<Invoice>, QuerydslPredicateExecutor<Invoice> {

    fun findByIdAndCustomerWorkspace(id: Long, workspace: Workspace): Invoice?

    fun findByIncome(income: Income): Invoice?
}