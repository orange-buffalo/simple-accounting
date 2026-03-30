package io.orangebuffalo.simpleaccounting.business.customers.impl

import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.business.customers.CustomersRepositoryExt
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class CustomersRepositoryExtImpl(
    private val dslContext: DSLContext
) : CustomersRepositoryExt {

    private val customer = Tables.CUSTOMER

    override fun findByWorkspaceIdPaginated(
        workspaceId: Long,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<Customer> {
        var query = dslContext
            .select(*customer.fields())
            .from(customer)
            .where(customer.workspaceId.eq(workspaceId))

        if (afterCreatedAt != null) {
            query = query.and(customer.createdAt.gt(afterCreatedAt))
        }

        return query
            .orderBy(customer.createdAt.asc())
            .limit(limit + 1)
            .fetchListOf()
    }

    override fun countByWorkspaceId(workspaceId: Long): Int = dslContext
        .selectCount()
        .from(customer)
        .where(customer.workspaceId.eq(workspaceId))
        .fetchOne(0, Int::class.java)!!
}
