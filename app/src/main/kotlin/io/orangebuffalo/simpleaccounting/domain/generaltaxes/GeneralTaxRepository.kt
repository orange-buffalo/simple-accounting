package io.orangebuffalo.simpleaccounting.domain.generaltaxes

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface GeneralTaxRepository : AbstractEntityRepository<GeneralTax> {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): GeneralTax?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
