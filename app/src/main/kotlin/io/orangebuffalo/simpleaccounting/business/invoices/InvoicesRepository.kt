package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface InvoicesRepository : AbstractEntityRepository<Invoice>, InvoicesRepositoryExt

interface InvoicesRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Invoice?
    fun findAllOverdue() : List<Invoice>
}
