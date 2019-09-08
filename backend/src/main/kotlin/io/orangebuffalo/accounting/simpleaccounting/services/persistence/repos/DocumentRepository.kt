package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface DocumentRepository : AbstractEntityRepository<Document>, QuerydslPredicateExecutor<Document> {

    fun findByIdAndWorkspace(id: Long, workspace: Workspace): Document?

}