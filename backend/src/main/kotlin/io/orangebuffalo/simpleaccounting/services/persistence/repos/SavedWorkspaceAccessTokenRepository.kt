package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import java.time.Instant

interface SavedWorkspaceAccessTokenRepository
    : AbstractEntityRepository<SavedWorkspaceAccessToken>, SavedWorkspaceAccessTokenRepositoryExt

interface SavedWorkspaceAccessTokenRepositoryExt {

    fun findByWorkspaceAccessTokenAndOwner(
        workspaceAccessTokenId: Long,
        ownerId: Long
    ): SavedWorkspaceAccessToken?

    fun findAllValidByOwner(
        owner: String,
        currentTime: Instant
    ): List<SavedWorkspaceAccessToken>

    fun findWorkspaceByValidTokenOwnerAndId(
        owner: String,
        workspaceId: Long,
        currentTime: Instant
    ): Workspace?

    fun findWorkspacesByValidTokenOwner(
        owner: String,
        currentTime: Instant
    ): List<Workspace>
}
