package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import kotlinx.coroutines.Deferred
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository
) {

    suspend fun getWorkspaceAsync(workspaceId: Long): Deferred<Workspace?> =
        withDbContextAsync {
            workspaceRepository.findById(workspaceId).orElse(null)
        }

    suspend fun getUserWorkspaces(userName: String): List<Workspace> =
        withDbContext {
            workspaceRepository.findAllByOwnerUserName(userName)
        }

    suspend fun createWorkspace(workspace: Workspace): Workspace =
        withDbContext {
            workspaceRepository.save(workspace)
        }

    suspend fun save(workspace: Workspace) =
        withDbContext {
            workspaceRepository.save(workspace)
        }
}