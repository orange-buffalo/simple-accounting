package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Tax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TaxRepository : AbstractEntityRepository<Tax>, QuerydslPredicateExecutor<Tax> {
    fun findByIdAndWorkspace(id: Long, workspace: Workspace) : Tax?
}