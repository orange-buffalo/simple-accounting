package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface ExpenseRepository : AbstractEntityRepository<Expense>, QuerydslPredicateExecutor<Expense> {
    fun findByIdAndCategoryWorkspace(id: Long, workspace: Workspace) : Expense?
}