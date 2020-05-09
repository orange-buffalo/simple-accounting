package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.WorkspaceAccessTokenRepositoryExt
import org.jooq.DSLContext
import org.jooq.impl.DSL.trueCondition
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class WorkspaceAccessTokenRepositoryExtImpl(
    private val dslContext: DSLContext
) : WorkspaceAccessTokenRepositoryExt {

    private val workspaceAccessToken = Tables.WORKSPACE_ACCESS_TOKEN

    override fun findValidByToken(token: String, currentTime: Instant): WorkspaceAccessToken? = dslContext
        .select()
        .from(workspaceAccessToken)
        .where(
            workspaceAccessToken.token.eq(token),
            workspaceAccessToken.validTill.greaterThan(currentTime),
            workspaceAccessToken.revoked.isFalse
        )
        .fetchOneOrNull()

    override fun findWorkspaceByValidToken(
        token: String,
        currentTime: Instant,
        workspaceId: Long?
    ): Workspace? {
        val workspace = Tables.WORKSPACE
        return dslContext
            .select(*workspace.fields())
            .from(workspace)
            .innerJoin(workspaceAccessToken).on(workspaceAccessToken.workspaceId.eq(workspace.id))
            .where(
                workspaceAccessToken.token.eq(token),
                workspaceAccessToken.validTill.greaterThan(currentTime),
                workspaceAccessToken.revoked.isFalse,
                if (workspaceId == null) trueCondition() else workspace.id.eq(workspaceId)
            )
            .fetchOneOrNull()
    }
}
