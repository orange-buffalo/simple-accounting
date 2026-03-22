package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import java.time.Instant

interface WorkspacesRepository : AbstractEntityRepository<Workspace>, WorkspacesRepositoryExt

interface WorkspacesRepositoryExt {
    fun findAllByOwnerUserName(userName: String): List<Workspace>

    fun findByIdAndOwnerUserName(workspaceId: Long, userName: String): Workspace?

    fun findByOwnerUserNamePaginated(
        userName: String,
        limit: Int,
        afterCreatedAt: Instant?,
    ): List<Workspace>

    fun countByOwnerUserName(userName: String): Int
}
