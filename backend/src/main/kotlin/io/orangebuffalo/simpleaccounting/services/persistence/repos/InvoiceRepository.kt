package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import org.springframework.data.repository.CrudRepository

interface InvoiceRepository : CrudRepository<Invoice, Long>, InvoiceRepositoryExt

interface InvoiceRepositoryExt {
    fun findByIdAndWorkspace(id: Long, workspace: Long): Invoice?

    fun findByIncome(income: Long): Invoice?
}
