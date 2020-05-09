package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.SavedWorkspaceAccessTokenRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class SavedWorkspaceAccessTokenRepositoryExtImpl(
    private val dslContext: DSLContext
) : SavedWorkspaceAccessTokenRepositoryExt {

    private val savedWorkspaceAccessToken = Tables.SAVED_WORKSPACE_ACCESS_TOKEN

    override fun findByWorkspaceAccessTokenAndOwner(
        workspaceAccessTokenId: Long,
        ownerId: Long
    ): SavedWorkspaceAccessToken? = dslContext
        .select()
        .from(savedWorkspaceAccessToken)
        .where(
            savedWorkspaceAccessToken.workspaceAccessTokenId.eq(workspaceAccessTokenId),
            savedWorkspaceAccessToken.ownerId.eq(ownerId)
        )
        .fetchOneOrNull()

    override fun findAllValidByOwner(owner: String, currentTime: Instant): List<SavedWorkspaceAccessToken> {
        return dslContext
            .select()
            .from(savedWorkspaceAccessToken)
            .where(
                savedWorkspaceAccessToken.platformUser().userName.eq(owner),
                savedWorkspaceAccessToken.workspaceAccessToken().validTill.greaterThan(currentTime),
                savedWorkspaceAccessToken.workspaceAccessToken().revoked.isFalse
            )
            .fetchListOf()
    }

    override fun findWorkspaceByValidTokenOwnerAndId(
        owner: String,
        workspaceId: Long,
        currentTime: Instant
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
                workspaceAccessToken.validTill.greaterThan(currentTime),
                workspaceAccessToken.revoked.isFalse,
                workspace.id.eq(workspaceId)
            )
            .fetchOneOrNull()
    }

    override fun findWorkspacesByValidTokenOwner(owner: String, currentTime: Instant): List<Workspace> {
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
                workspaceAccessToken.validTill.greaterThan(currentTime),
                workspaceAccessToken.revoked.isFalse
            )
            .fetchListOf()
    }
}
