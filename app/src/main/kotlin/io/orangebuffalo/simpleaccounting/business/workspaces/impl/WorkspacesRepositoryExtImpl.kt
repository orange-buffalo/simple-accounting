package io.orangebuffalo.simpleaccounting.business.workspaces.impl

import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchListOf
import io.orangebuffalo.simpleaccounting.infra.jooq.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WorkspacesRepositoryExtImpl(
    private val dslContext: DSLContext
) : WorkspacesRepositoryExt {

    private val workspace = Tables.WORKSPACE

    override fun findAllByOwnerUserName(userName: String): List<Workspace> {
        val owner = Tables.PLATFORM_USER
        return dslContext
            .select(*workspace.fields())
            .from(workspace)
            .join(owner).on(owner.id.eq(workspace.ownerId))
            .where(owner.userName.eq(userName))
            .fetchListOf()
    }

    override fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace? {
        val owner = Tables.PLATFORM_USER
        return dslContext
            .select(*workspace.fields())
            .from(workspace)
            .join(owner).on(owner.id.eq(workspace.ownerId))
            .where(
                owner.userName.eq(userName),
                workspace.id.eq(workspaceId)
            )
            .fetchOneOrNull()
    }
}
