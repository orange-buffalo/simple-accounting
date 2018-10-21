package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category

interface CategoryRepository : AbstractEntityRepository<Category> {

    fun findAllByWorkspaceOwnerUserName(usrName: String): List<Category>
}