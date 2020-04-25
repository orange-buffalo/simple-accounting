package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice

interface InvoiceRepository : AbstractEntityRepository<Invoice>, InvoiceRepositoryExt

interface InvoiceRepositoryExt {
    fun findByIdAndWorkspace(id: Long, workspace: Long): Invoice?

    fun findByIncome(income: Long): Invoice?
}
