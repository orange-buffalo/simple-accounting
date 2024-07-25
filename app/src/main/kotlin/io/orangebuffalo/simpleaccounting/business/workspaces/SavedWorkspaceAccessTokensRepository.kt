package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface SavedWorkspaceAccessTokensRepository
    : AbstractEntityRepository<SavedWorkspaceAccessToken>, SavedWorkspaceAccessTokensRepositoryExt {

    fun findByWorkspaceAccessTokenIdAndOwnerId(
        workspaceAccessTokenId: Long,
        ownerId: Long
    ): SavedWorkspaceAccessToken?
}

interface SavedWorkspaceAccessTokensRepositoryExt {

    fun findAllValidByOwner(owner: String): List<SavedWorkspaceAccessToken>

    fun findWorkspaceByValidTokenOwnerAndId(owner: String, workspaceId: Long): Workspace?

    fun findWorkspacesByValidTokenOwner(owner: String): List<Workspace>
}
