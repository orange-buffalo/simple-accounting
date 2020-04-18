package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Customer
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface CustomerRepository : LegacyAbstractEntityRepository<Customer>, QuerydslPredicateExecutor<Customer> {
    fun findByIdAndWorkspace(id: Long, workspace: Workspace) : Customer?
}
