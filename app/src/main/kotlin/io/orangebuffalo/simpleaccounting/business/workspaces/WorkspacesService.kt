package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.ensureRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class WorkspacesService(
    private val workspacesRepository: WorkspacesRepository,
    private val workspaceAccessTokensRepository: WorkspaceAccessTokensRepository,
    private val savedWorkspaceAccessTokensRepository: SavedWorkspaceAccessTokensRepository,
    private val platformUsersService: PlatformUsersService
) {

    fun getUserWorkspaces(userName: String): List<Workspace> = workspacesRepository.findAllByOwnerUserName(userName)

    fun createWorkspace(workspace: Workspace): Workspace = workspacesRepository.save(workspace)

    fun save(workspace: Workspace) = workspacesRepository.save(workspace)

    fun saveSharedWorkspace(token: String): Workspace {
        val accessToken = getValidWorkspaceAccessToken(token)
        val currentUser = platformUsersService.getCurrentUser()
        val savedWorkspaceAccessToken = savedWorkspaceAccessTokensRepository.findByWorkspaceAccessTokenIdAndOwnerId(
            accessToken.id!!, currentUser.id!!
        )

        if (savedWorkspaceAccessToken == null) {
            savedWorkspaceAccessTokensRepository.save(
                SavedWorkspaceAccessToken(
                    workspaceAccessTokenId = accessToken.id!!,
                    ownerId = currentUser.id!!
                )
            )
        }

        return workspacesRepository.findByIdOrNull(accessToken.workspaceId)
            ?: throw EntityNotFoundException("Workspace is not found for $token")
    }

    fun getValidWorkspaceAccessToken(token: String): WorkspaceAccessToken =
        workspaceAccessTokensRepository.findValidByToken(token) ?: throw InvalidWorkspaceAccessTokenException(token)

    fun getWorkspaceByValidAccessToken(token: String): Workspace =
        workspaceAccessTokensRepository.findWorkspaceByValidToken(token) ?: throw InvalidWorkspaceAccessTokenException(token)

    fun getSharedWorkspaces(): List<Workspace> =
        savedWorkspaceAccessTokensRepository
            .findWorkspacesByValidTokenOwner(
                ensureRegularUserPrincipal().userName
            )

    fun validateWorkspaceAccess(
        workspaceId: String,
        accessMode: WorkspaceAccessMode
    ) {
        getAccessibleWorkspace(workspaceId, accessMode)
    }

    fun getAccessibleWorkspace(
        workspaceId: String,
        accessMode: WorkspaceAccessMode
    ): Workspace {

        val currentPrincipal = getCurrentPrincipal()

        return if (currentPrincipal.isTransient) {
            getAccessibleWorkspaceForTransientUser(accessMode, workspaceId, currentPrincipal)
        } else {
            getAccessibleWorkspaceForRegularUser(workspaceId, currentPrincipal, accessMode)
        }
    }

    private fun getAccessibleWorkspaceForRegularUser(
        workspaceId: String,
        currentPrincipal: SecurityPrincipal,
        accessMode: WorkspaceAccessMode
    ): Workspace {
        val ownWorkspace = workspacesRepository.findByIdAndOwnerUserName(workspaceId, currentPrincipal.userName)

        val sharedWorkspace = if (accessMode == WorkspaceAccessMode.READ_ONLY) {
            savedWorkspaceAccessTokensRepository.findWorkspaceByValidTokenOwnerAndId(
                currentPrincipal.userName, workspaceId
            )
        } else null

        return ownWorkspace
            ?: sharedWorkspace
            ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
    }

    private fun getAccessibleWorkspaceForTransientUser(
        accessMode: WorkspaceAccessMode,
        workspaceId: String,
        currentPrincipal: SecurityPrincipal
    ): Workspace {
        if (accessMode != WorkspaceAccessMode.READ_ONLY) {
            throw EntityNotFoundException("Workspace $workspaceId is not found")
        }

        return workspaceAccessTokensRepository
                .findWorkspaceByValidToken(
                    currentPrincipal.userName, workspaceId
                )
                ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
    }

    fun getWorkspace(workspaceId: String): Workspace = workspacesRepository.findById(workspaceId)
        .orElseThrow { EntityNotFoundException("Workspace $workspaceId is not found") }
}

enum class WorkspaceAccessMode {
    ADMIN,
    READ_ONLY,
    READ_WRITE
}
