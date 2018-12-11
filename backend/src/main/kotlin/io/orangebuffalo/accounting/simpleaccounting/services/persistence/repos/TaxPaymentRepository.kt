package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.TaxPayment
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TaxPaymentRepository : AbstractEntityRepository<TaxPayment>, QuerydslPredicateExecutor<TaxPayment> {
    fun findByIdAndWorkspace(id: Long, workspace: Workspace) : TaxPayment?
}