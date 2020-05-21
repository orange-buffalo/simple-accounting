package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document

interface DocumentRepository : AbstractEntityRepository<Document>, DocumentRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Document?
}

interface DocumentRepositoryExt {
    fun findValidIds(ids: Collection<Long>, workspaceId: Long): Collection<Long>
}
