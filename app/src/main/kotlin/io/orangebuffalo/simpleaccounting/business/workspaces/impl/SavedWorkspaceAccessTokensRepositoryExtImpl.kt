package io.orangebuffalo.simpleaccounting.business.workspaces.impl

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.workspaces.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.workspaces.SavedWorkspaceAccessTokensRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class SavedWorkspaceAccessTokensRepositoryExtImpl(
    private val dslContext: DSLContext,
    private val timeService: TimeService
) : SavedWorkspaceAccessTokensRepositoryExt {

    private val savedWorkspaceAccessToken = Tables.SAVED_WORKSPACE_ACCESS_TOKEN

    override fun findAllValidByOwner(owner: String): List<SavedWorkspaceAccessToken> {
        return dslContext
            .select()
            .from(savedWorkspaceAccessToken)
            .where(
                savedWorkspaceAccessToken.platformUser().userName.eq(owner),
                savedWorkspaceAccessToken.workspaceAccessToken().validTill
                    .greaterThan(timeService.currentTime()),
                savedWorkspaceAccessToken.workspaceAccessToken().revoked.isFalse
            )
            .fetchListOf()
    }

    override fun findWorkspaceByValidTokenOwnerAndId(
        owner: String,
        workspaceId: Long
    ): Workspace? {
        val tokenOwner = Tables.PLATFORM_USER
        val workspace = Tables.WORKSPACE
        val workspaceAccessToken = Tables.WORKSPACE_ACCESS_TOKEN
        return dslContext
            .select(*workspace.fields())
            .from(savedWorkspaceAccessToken)
            .join(workspaceAccessToken).on(workspaceAccessToken.id.eq(savedWorkspaceAccessToken.workspaceAccessTokenId))
            .join(workspace).on(workspace.id.eq(workspaceAccessToken.workspaceId))
            .join(tokenOwner).on(tokenOwner.id.eq(savedWorkspaceAccessToken.ownerId))
            .where(
                tokenOwner.userName.eq(owner),
                workspaceAccessToken.validTill.greaterThan(timeService.currentTime()),
                workspaceAccessToken.revoked.isFalse,
                workspace.id.eq(workspaceId)
            )
            .fetchOneOrNull()
    }

    override fun findWorkspacesByValidTokenOwner(owner: String): List<Workspace> {
        val tokenOwner = Tables.PLATFORM_USER
        val workspace = Tables.WORKSPACE
        val workspaceAccessToken = Tables.WORKSPACE_ACCESS_TOKEN
        return dslContext
            .select(*workspace.fields())
            .from(savedWorkspaceAccessToken)
            .join(workspaceAccessToken).on(workspaceAccessToken.id.eq(savedWorkspaceAccessToken.workspaceAccessTokenId))
            .join(workspace).on(workspace.id.eq(workspaceAccessToken.workspaceId))
            .join(tokenOwner).on(tokenOwner.id.eq(savedWorkspaceAccessToken.ownerId))
            .where(
                tokenOwner.userName.eq(owner),
                workspaceAccessToken.validTill.greaterThan(timeService.currentTime()),
                workspaceAccessToken.revoked.isFalse
            )
            .fetchListOf()
    }
}
