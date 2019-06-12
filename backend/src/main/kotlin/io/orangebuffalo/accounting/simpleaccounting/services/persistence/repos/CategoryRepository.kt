package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface CategoryRepository : AbstractEntityRepository<Category>, QuerydslPredicateExecutor<Category> {

    fun findAllByWorkspaceOwnerUserName(usrName: String): List<Category>

    fun findAllByWorkspace(workspace: Workspace): List<Category>
}