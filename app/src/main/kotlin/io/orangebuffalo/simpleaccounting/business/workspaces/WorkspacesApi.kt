package io.orangebuffalo.simpleaccounting.business.workspaces

import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/")
class WorkspacesApi(
    private val platformUsersService: PlatformUsersService,
    private val workspacesService: WorkspacesService
) {

    @GetMapping("workspaces")
    suspend fun getWorkspaces(): List<WorkspaceDto> {
        val currentPrincipal = getCurrentPrincipal()
        return if (currentPrincipal.isTransient) {
            workspacesService
                .getWorkspaceByValidAccessToken(currentPrincipal.userName)
                .let { listOf(it.mapToWorkspaceDto(false)) }
        } else {
            workspacesService
                .getUserWorkspaces(currentPrincipal.userName)
                .map { it.mapToWorkspaceDto(true) }
        }
    }

    @PostMapping("workspaces")
    suspend fun createWorkspace(
        @RequestBody @Valid createWorkspaceRequest: CreateWorkspaceDto
    ): WorkspaceDto = workspacesService
        .createWorkspace(
            Workspace(
                name = createWorkspaceRequest.name,
                defaultCurrency = createWorkspaceRequest.defaultCurrency,
                ownerId = platformUsersService.getCurrentUser().id!!
            )
        )
        .mapToWorkspaceDto(true)

    @PutMapping("workspaces/{workspaceId}")
    suspend fun editWorkspace(
        @RequestBody @Valid editWorkspaceRequest: EditWorkspaceDto,
        @PathVariable workspaceId: Long
    ): WorkspaceDto = workspacesService
        .getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        .apply {
            name = editWorkspaceRequest.name
        }
        .let { workspacesService.save(it) }
        .mapToWorkspaceDto(true)

    @GetMapping("shared-workspaces")
    suspend fun getSharedWorkspaces(): List<WorkspaceDto> = workspacesService
        .getSharedWorkspaces().map { it.mapToWorkspaceDto(false) }

    @PostMapping("shared-workspaces")
    suspend fun saveSharedWorkspace(@Valid @RequestBody request: SaveSharedWorkspaceRequestDto): WorkspaceDto =
        workspacesService.saveSharedWorkspace(request.token).mapToWorkspaceDto(false)
}

data class WorkspaceDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var defaultCurrency: String,
    var editable: Boolean
)

data class CreateWorkspaceDto(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 3) val defaultCurrency: String
)

data class EditWorkspaceDto(
    @field:NotBlank @field:Size(max = 255) val name: String
)

data class SaveSharedWorkspaceRequestDto(
    @field:NotBlank val token: String
)

private fun Workspace.mapToWorkspaceDto(editable: Boolean) = WorkspaceDto(
    name = name,
    id = id,
    version = version!!,
    defaultCurrency = defaultCurrency,
    editable = editable
)
