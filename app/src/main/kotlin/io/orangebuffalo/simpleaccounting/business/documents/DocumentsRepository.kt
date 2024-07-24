package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface DocumentsRepository : AbstractEntityRepository<Document>, DocumentsRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Document?
}

interface DocumentsRepositoryExt {
    fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long>
}
