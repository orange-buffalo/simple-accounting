package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface WorkspacesRepository : AbstractEntityRepository<Workspace>, WorkspacesRepositoryExt

interface WorkspacesRepositoryExt {
    fun findAllByOwnerUserName(userName: String): List<Workspace>

    fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace?
}
