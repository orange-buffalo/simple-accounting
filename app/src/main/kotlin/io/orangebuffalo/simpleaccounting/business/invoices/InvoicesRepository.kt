package io.orangebuffalo.simpleaccounting.business.invoices

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import org.springframework.data.jdbc.repository.query.Query

interface InvoicesRepository : AbstractEntityRepository<Invoice>, InvoicesRepositoryExt {
    @Query("""
        select i.* from invoice i join customer c on c.id = i.customer_id
        where i.id in (:ids) and c.workspace_id = :workspaceId
    """)
    fun findAllByIdsAndWorkspaceId(ids: List<String>, workspaceId: String): List<Invoice>
}

interface InvoicesRepositoryExt {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): Invoice?
    fun findAllOverdue() : List<Invoice>
}
