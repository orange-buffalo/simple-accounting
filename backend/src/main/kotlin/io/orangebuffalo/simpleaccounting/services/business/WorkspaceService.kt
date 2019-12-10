package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.simpleaccounting.services.persistence.entities.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.repos.SavedWorkspaceAccessTokenRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.WorkspaceAccessTokenRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.WorkspaceRepository
import io.orangebuffalo.simpleaccounting.services.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.services.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.services.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.web.api.integration.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceAccessTokenRepository: WorkspaceAccessTokenRepository,
    private val savedWorkspaceAccessTokenRepository: SavedWorkspaceAccessTokenRepository,
    private val platformUserService: PlatformUserService,
    private val timeService: TimeService
) {

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
                    ensureRegularUserPrincipal().userName,
                    timeService.currentTime()
                )
                .map { it.workspaceAccessToken.workspace }
        }

    suspend fun getAccessibleWorkspace(
        workspaceId: Long,
        accessMode: WorkspaceAccessMode
    ): Workspace {

        val currentPrincipal = getCurrentPrincipal()

        return if (currentPrincipal.isTransient) {
            getAccessibleWorkspaceForTransientUser(accessMode, workspaceId, currentPrincipal)
        } else {
            getAccessibleWorkspaceForRegularUser(workspaceId, currentPrincipal, accessMode)
        }
    }

    private suspend fun getAccessibleWorkspaceForRegularUser(
        workspaceId: Long,
        currentPrincipal: SecurityPrincipal,
        accessMode: WorkspaceAccessMode
    ): Workspace {
        val ownWorkspaceAsync = withDbContextAsync {
            workspaceRepository.findByIdAndOwnerUserName(workspaceId, currentPrincipal.userName)
        }

        val sharedWorkspace = if (accessMode == WorkspaceAccessMode.READ_ONLY) {
            withDbContext {
                savedWorkspaceAccessTokenRepository.findValidByOwnerAndWorkspaceWithFetchedWorkspace(
                    currentPrincipal.userName, workspaceId, timeService.currentTime()
                )
            }?.workspaceAccessToken?.workspace
        } else null

        return ownWorkspaceAsync.await()
            ?: sharedWorkspace
            ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
    }

    private suspend fun getAccessibleWorkspaceForTransientUser(
        accessMode: WorkspaceAccessMode,
        workspaceId: Long,
        currentPrincipal: SecurityPrincipal
    ): Workspace {
        if (accessMode != WorkspaceAccessMode.READ_ONLY) {
            throw EntityNotFoundException("Workspace $workspaceId is not found")
        }

        return withDbContext {
            workspaceAccessTokenRepository
                .findValidByTokenAndWorkspaceWithFetchedWorkspace(
                    currentPrincipal.userName, timeService.currentTime(), workspaceId
                )
                ?.workspace
                ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
        }
    }
}

enum class WorkspaceAccessMode {
    ADMIN,
    READ_ONLY,
    READ_WRITE
}
