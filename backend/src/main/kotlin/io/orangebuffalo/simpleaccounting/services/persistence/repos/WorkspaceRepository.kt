package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace

interface WorkspaceRepository : LegacyAbstractEntityRepository<Workspace> {

    fun findAllByOwnerUserName(userName: String): List<Workspace>

    fun findByIdAndOwnerUserName(workspaceId: Long, owner: String): Workspace?
}
