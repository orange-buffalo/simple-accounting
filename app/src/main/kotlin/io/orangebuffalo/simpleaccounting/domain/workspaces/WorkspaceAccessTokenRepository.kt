package io.orangebuffalo.simpleaccounting.domain.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface WorkspaceAccessTokenRepository
    : AbstractEntityRepository<WorkspaceAccessToken>, WorkspaceAccessTokenRepositoryExt

interface WorkspaceAccessTokenRepositoryExt {

    fun findValidByToken(token: String): WorkspaceAccessToken?

    fun findWorkspaceByValidToken(token: String, workspaceId: Long? = null): Workspace?
}
