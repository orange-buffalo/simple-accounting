package io.orangebuffalo.simpleaccounting.domain.workspaces.impl

import io.orangebuffalo.simpleaccounting.domain.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.domain.workspaces.WorkspaceRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WorkspaceRepositoryExtImpl(
    private val dslContext: DSLContext
) : WorkspaceRepositoryExt {

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
