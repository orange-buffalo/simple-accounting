package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Customer
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CustomerRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CustomerRepositoryExtImpl(
    private val dslContext: DSLContext
) : CustomerRepositoryExt {

    private val customer = Tables.CUSTOMER

    override fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): Customer? = dslContext
        .select()
        .from(customer)
        .where(
            customer.id.eq(id),
            customer.workspaceId.eq(workspaceId)
        )
        .fetchOneOrNull()

    override fun existsByIdAndWorkspaceId(customerId: Long, workspaceId: Long): Boolean = dslContext
        .fetchExists(
            customer,
            customer.id.eq(customerId),
            customer.workspaceId.eq(workspaceId)
        )
}
