package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import java.time.Instant

interface WorkspaceAccessTokenRepository
    : AbstractEntityRepository<WorkspaceAccessToken>, WorkspaceAccessTokenRepositoryExt

interface WorkspaceAccessTokenRepositoryExt {

    // todo #222: here an in saved token repo move time from params to impl as injection
    fun findValidByToken(token: String, currentTime: Instant): WorkspaceAccessToken?

    fun findWorkspaceByValidToken(token: String, currentTime: Instant, workspaceId: Long? = null): Workspace?
}
