package io.orangebuffalo.simpleaccounting.domain.workspaces

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface SavedWorkspaceAccessTokenRepository
    : AbstractEntityRepository<SavedWorkspaceAccessToken>, SavedWorkspaceAccessTokenRepositoryExt {

    fun findByWorkspaceAccessTokenIdAndOwnerId(
        workspaceAccessTokenId: Long,
        ownerId: Long
    ): SavedWorkspaceAccessToken?
}

interface SavedWorkspaceAccessTokenRepositoryExt {

    fun findAllValidByOwner(owner: String): List<SavedWorkspaceAccessToken>

    fun findWorkspaceByValidTokenOwnerAndId(owner: String, workspaceId: Long): Workspace?

    fun findWorkspacesByValidTokenOwner(owner: String): List<Workspace>
}
