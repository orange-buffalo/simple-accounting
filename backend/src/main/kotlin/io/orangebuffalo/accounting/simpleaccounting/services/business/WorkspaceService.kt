package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.getCurrentPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.SavedWorkspaceAccessTokenRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceAccessTokenRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
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
                    getCurrentPrincipal().username,
                    timeService.currentTime()
                )
                .map { it.workspaceAccessToken.workspace }
        }

    suspend fun getAccessibleWorkspace(
        workspaceId: Long,
        accessMode: WorkspaceAccessMode
    ): Workspace {
        val ownWorkspaceAsync = withDbContextAsync {
            workspaceRepository.findByIdAndOwnerUserName(workspaceId, getCurrentPrincipal().username)
        }

        val sharedWorkspace = if (accessMode == WorkspaceAccessMode.READ_ONLY) {
            withDbContext {
                savedWorkspaceAccessTokenRepository.findValidByOwnerAndWorkspaceWithFetchedWorkspace(
                    getCurrentPrincipal().username, workspaceId, timeService.currentTime()
                )
            }?.workspaceAccessToken?.workspace
        } else null

        return ownWorkspaceAsync.await()
            ?: sharedWorkspace
            ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
    }
}

enum class WorkspaceAccessMode {
    ADMIN,
    READ_ONLY,
    READ_WRITE
}