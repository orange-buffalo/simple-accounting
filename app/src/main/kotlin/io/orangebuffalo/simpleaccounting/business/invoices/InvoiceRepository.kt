package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface InvoiceRepository : AbstractEntityRepository<Invoice>, InvoiceRepositoryExt

interface InvoiceRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Invoice?
    fun findAllOverdue() : List<Invoice>
}
