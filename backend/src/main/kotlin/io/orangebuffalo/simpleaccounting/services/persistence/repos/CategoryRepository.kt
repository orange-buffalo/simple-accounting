package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface CategoryRepository : LegacyAbstractEntityRepository<Category>, QuerydslPredicateExecutor<Category> {

    fun findAllByWorkspaceOwnerUserName(usrName: String): List<Category>

    fun findAllByWorkspace(workspace: Workspace): List<Category>

    fun existsByWorkspaceIdAndId(workspaceId: Long, id: Long): Boolean
}
