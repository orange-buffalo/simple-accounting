package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax

interface GeneralTaxRepository : AbstractEntityRepository<GeneralTax>, GeneralTaxRepositoryExt

interface GeneralTaxRepositoryExt {
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): GeneralTax?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
