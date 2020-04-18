package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface DocumentRepository : LegacyAbstractEntityRepository<Document>, QuerydslPredicateExecutor<Document> {

    fun findByIdAndWorkspace(id: Long, workspace: Workspace): Document?

    fun findByWorkspaceIdAndIdIsIn(workspaceId: Long, ids: Collection<Long>) : Collection<DocumentId>

}

interface DocumentId {
    val id: Long
}
