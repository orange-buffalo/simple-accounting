package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface InvoicesRepository : AbstractEntityRepository<Invoice>, InvoicesRepositoryExt

interface InvoicesRepositoryExt {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): Invoice?
    fun findAllOverdue() : List<Invoice>
}
