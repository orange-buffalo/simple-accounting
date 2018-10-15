package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.repository.CrudRepository

interface WorkspaceRepository : CrudRepository<Workspace, Long> {

    fun findAllByOwnerUserName(userName: String): List<Workspace>
}