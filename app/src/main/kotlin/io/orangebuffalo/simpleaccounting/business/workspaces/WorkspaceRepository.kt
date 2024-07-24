package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface WorkspaceRepository : AbstractEntityRepository<Workspace>, WorkspaceRepositoryExt

interface WorkspaceRepositoryExt {
    fun findAllByOwnerUserName(userName: String): List<Workspace>

    fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace?
}
