package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface GeneralTaxesRepository : AbstractEntityRepository<GeneralTax> {
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): GeneralTax?
    fun existsByIdAndWorkspaceId(id: String, workspaceId: String): Boolean
}
