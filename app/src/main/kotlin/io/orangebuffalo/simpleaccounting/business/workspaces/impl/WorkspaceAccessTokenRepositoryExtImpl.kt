package io.orangebuffalo.simpleaccounting.business.workspaces.impl

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokenRepositoryExt
import org.jooq.DSLContext
import org.jooq.impl.DSL.trueCondition
import org.springframework.stereotype.Repository

@Repository
class WorkspaceAccessTokenRepositoryExtImpl(
    private val dslContext: DSLContext,
    private val timeService: TimeService
) : WorkspaceAccessTokenRepositoryExt {

    private val workspaceAccessToken = Tables.WORKSPACE_ACCESS_TOKEN

    override fun findValidByToken(token: String): WorkspaceAccessToken? = dslContext
        .select()
        .from(workspaceAccessToken)
        .where(
            workspaceAccessToken.token.eq(token),
            workspaceAccessToken.validTill.greaterThan(timeService.currentTime()),
            workspaceAccessToken.revoked.isFalse
        )
        .fetchOneOrNull()

    override fun findWorkspaceByValidToken(
        token: String,
        workspaceId: Long?
    ): Workspace? {
        val workspace = Tables.WORKSPACE
        return dslContext
            .select(*workspace.fields())
            .from(workspace)
            .innerJoin(workspaceAccessToken).on(workspaceAccessToken.workspaceId.eq(workspace.id))
            .where(
                workspaceAccessToken.token.eq(token),
                workspaceAccessToken.validTill.greaterThan(timeService.currentTime()),
                workspaceAccessToken.revoked.isFalse,
                if (workspaceId == null) trueCondition() else workspace.id.eq(workspaceId)
            )
            .fetchOneOrNull()
    }
}
