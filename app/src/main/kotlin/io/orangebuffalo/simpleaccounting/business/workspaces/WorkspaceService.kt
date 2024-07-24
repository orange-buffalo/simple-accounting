package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.users.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceAccessTokenRepository: WorkspaceAccessTokenRepository,
    private val savedWorkspaceAccessTokenRepository: SavedWorkspaceAccessTokenRepository,
    private val platformUserService: PlatformUserService
) {

    suspend fun getUserWorkspaces(userName: String): List<Workspace> = withDbContext {
        workspaceRepository.findAllByOwnerUserName(userName)
    }

    suspend fun createWorkspace(workspace: Workspace): Workspace = withDbContext {
        workspaceRepository.save(workspace)
    }

    suspend fun save(workspace: Workspace) = withDbContext {
        workspaceRepository.save(workspace)
    }

    suspend fun saveSharedWorkspace(token: String): Workspace = withDbContext {
        val accessToken = getValidWorkspaceAccessToken(token)
        val currentUser = platformUserService.getCurrentUser()
        val savedWorkspaceAccessToken = savedWorkspaceAccessTokenRepository.findByWorkspaceAccessTokenIdAndOwnerId(
            accessToken.id!!, currentUser.id!!
        )

        if (savedWorkspaceAccessToken == null) {
            savedWorkspaceAccessTokenRepository.save(
                SavedWorkspaceAccessToken(
                    workspaceAccessTokenId = accessToken.id!!,
                    ownerId = currentUser.id!!
                )
            )
        }

        workspaceRepository.findByIdOrNull(accessToken.workspaceId)
            ?: throw EntityNotFoundException("Workspace is not found for $token")
    }

    suspend fun getValidWorkspaceAccessToken(token: String): WorkspaceAccessToken = withDbContext {
        workspaceAccessTokenRepository.findValidByToken(token)
            ?: throw InvalidWorkspaceAccessTokenException(token)
    }

    suspend fun getWorkspaceByValidAccessToken(token: String): Workspace = withDbContext {
        workspaceAccessTokenRepository.findWorkspaceByValidToken(token)
            ?: throw InvalidWorkspaceAccessTokenException(token)
    }

    suspend fun getSharedWorkspaces(): List<Workspace> = withDbContext {
        savedWorkspaceAccessTokenRepository
            .findWorkspacesByValidTokenOwner(
                ensureRegularUserPrincipal().userName
            )
    }

    suspend fun validateWorkspaceAccess(
        workspaceId: Long,
        accessMode: WorkspaceAccessMode
    ) {
        getAccessibleWorkspace(workspaceId, accessMode)
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
                savedWorkspaceAccessTokenRepository.findWorkspaceByValidTokenOwnerAndId(
                    currentPrincipal.userName, workspaceId
                )
            }
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
                .findWorkspaceByValidToken(
                    currentPrincipal.userName, workspaceId
                )
                ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
        }
    }

    suspend fun getWorkspace(workspaceId: Long): Workspace = withDbContext {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { EntityNotFoundException("Workspace $workspaceId is not found") }
    }
}

enum class WorkspaceAccessMode {
    ADMIN,
    READ_ONLY,
    READ_WRITE
}
