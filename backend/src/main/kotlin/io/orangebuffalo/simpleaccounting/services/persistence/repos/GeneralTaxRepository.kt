package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface GeneralTaxRepository : AbstractEntityRepository<GeneralTax>, QuerydslPredicateExecutor<GeneralTax> {
    fun findByIdAndWorkspace(id: Long, workspace: Workspace): GeneralTax?
}
