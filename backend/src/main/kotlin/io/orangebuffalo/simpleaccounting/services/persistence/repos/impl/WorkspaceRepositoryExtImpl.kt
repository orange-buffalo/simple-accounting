package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.fetchListOf
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.WorkspaceRepositoryExt
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
            .select()
            .from(workspace)
            .join(owner).on(owner.id.eq(workspace.ownerId))
            .where(owner.userName.eq(userName))
            .fetchListOf()
    }

    override fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace? {
        val owner = Tables.PLATFORM_USER
        return dslContext
            .select()
            .from(workspace)
            .join(owner).on(owner.id.eq(workspace.ownerId))
            .where(
                owner.userName.eq(userName),
                workspace.id.eq(workspaceId)
            )
            .fetchOneOrNull()
    }
}
