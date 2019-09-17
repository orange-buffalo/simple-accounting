package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.SavedWorkspaceAccessTokenRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceAccessTokenRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import kotlinx.coroutines.Deferred
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceAccessTokenRepository: WorkspaceAccessTokenRepository,
    private val savedWorkspaceAccessTokenRepository: SavedWorkspaceAccessTokenRepository,
    private val platformUserService: PlatformUserService,
    private val timeService: TimeService
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

    suspend fun saveSharedWorkspace(token: String): Workspace =
        withDbContext {
            val accessToken = getValidWorkspaceAccessToken(token)
            val currentUser = platformUserService.getCurrentUser()
            val savedWorkspaceAccessToken = savedWorkspaceAccessTokenRepository.findByWorkspaceAccessTokenAndOwner(
                accessToken, currentUser
            )

            if (savedWorkspaceAccessToken == null) {
                savedWorkspaceAccessTokenRepository.save(
                    SavedWorkspaceAccessToken(
                        workspaceAccessToken = accessToken,
                        owner = currentUser
                    )
                )
            }

            accessToken.workspace
        }

    suspend fun getValidWorkspaceAccessToken(token: String): WorkspaceAccessToken =
        withDbContext {
            workspaceAccessTokenRepository.findValidByToken(token, timeService.currentTime())
                ?: throw InvalidWorkspaceAccessTokenException(token)
        }

    suspend fun getSharedWorkspaces(): List<Workspace> =
        withDbContext {
            savedWorkspaceAccessTokenRepository
                .findAllValidByOwner(
                    platformUserService.getCurrentUser(),
                    timeService.currentTime()
                )
                .map { it.workspaceAccessToken.workspace }
        }
}