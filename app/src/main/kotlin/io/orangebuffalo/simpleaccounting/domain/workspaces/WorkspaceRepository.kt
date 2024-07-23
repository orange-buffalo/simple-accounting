package io.orangebuffalo.simpleaccounting.domain.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface WorkspaceRepository : AbstractEntityRepository<Workspace>, WorkspaceRepositoryExt

interface WorkspaceRepositoryExt {
    fun findAllByOwnerUserName(userName: String): List<Workspace>

    fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace?
}
