package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.GeneralTaxRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GeneralTaxRepositoryExtImpl(
    private val dslContext: DSLContext
) : GeneralTaxRepositoryExt {
    private val generalTax = Tables.GENERAL_TAX

    override fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): GeneralTax? = dslContext
        .select()
        .from(generalTax)
        .where(
            generalTax.id.eq(id),
            generalTax.workspaceId.eq(workspaceId)
        )
        .fetchOneOrNull()

    override fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean = dslContext
        .fetchExists(
            generalTax,
            generalTax.id.eq(id),
            generalTax.workspaceId.eq(workspaceId)
        )
}
