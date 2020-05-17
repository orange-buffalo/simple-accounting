package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken

interface WorkspaceAccessTokenRepository
    : AbstractEntityRepository<WorkspaceAccessToken>, WorkspaceAccessTokenRepositoryExt

interface WorkspaceAccessTokenRepositoryExt {

    fun findValidByToken(token: String): WorkspaceAccessToken?

    fun findWorkspaceByValidToken(token: String, workspaceId: Long? = null): Workspace?
}
