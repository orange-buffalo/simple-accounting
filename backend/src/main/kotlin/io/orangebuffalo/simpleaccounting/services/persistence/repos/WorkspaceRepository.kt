package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace

interface WorkspaceRepository : AbstractEntityRepository<Workspace>, WorkspaceRepositoryExt

interface WorkspaceRepositoryExt {
    fun findAllByOwnerUserName(userName: String): List<Workspace>

    fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace?
}
