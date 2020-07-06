package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice

interface InvoiceRepository : AbstractEntityRepository<Invoice>, InvoiceRepositoryExt

interface InvoiceRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Invoice?
}
