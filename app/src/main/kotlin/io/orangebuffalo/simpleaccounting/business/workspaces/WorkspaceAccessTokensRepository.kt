package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface WorkspaceAccessTokensRepository
    : AbstractEntityRepository<WorkspaceAccessToken>, WorkspaceAccessTokensRepositoryExt

interface WorkspaceAccessTokensRepositoryExt {

    fun findValidByToken(token: String): WorkspaceAccessToken?

    fun findWorkspaceByValidToken(token: String, workspaceId: Long? = null): Workspace?
}
