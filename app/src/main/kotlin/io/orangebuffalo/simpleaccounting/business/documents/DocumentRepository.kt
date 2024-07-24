package io.orangebuffalo.simpleaccounting.business.documents

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface DocumentRepository : AbstractEntityRepository<Document>, DocumentRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Document?
}

interface DocumentRepositoryExt {
    fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long>
}
