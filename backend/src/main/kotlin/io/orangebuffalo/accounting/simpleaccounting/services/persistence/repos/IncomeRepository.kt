package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface IncomeRepository : AbstractEntityRepository<Income>, QuerydslPredicateExecutor<Income> {
    fun findByIdAndCategoryWorkspace(id: Long, workspace: Workspace) : Income?
}