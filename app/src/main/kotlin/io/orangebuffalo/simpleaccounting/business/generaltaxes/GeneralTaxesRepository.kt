package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface GeneralTaxesRepository : AbstractEntityRepository<GeneralTax> {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): GeneralTax?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
