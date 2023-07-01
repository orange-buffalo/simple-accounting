package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.security.getCurrentPrincipal
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@RestController
@RequestMapping("/api/")
class WorkspacesApiController(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService
) {

    @GetMapping("workspaces")
    suspend fun getWorkspaces(): List<WorkspaceDto> {
        val currentPrincipal = getCurrentPrincipal()
        return if (currentPrincipal.isTransient) {
            workspaceService
                .getWorkspaceByValidAccessToken(currentPrincipal.userName)
                .let { listOf(it.mapToWorkspaceDto(false)) }
        } else {
            workspaceService
                .getUserWorkspaces(currentPrincipal.userName)
                .map { it.mapToWorkspaceDto(true) }
        }
    }

    @PostMapping("workspaces")
    suspend fun createWorkspace(
        @RequestBody @Valid createWorkspaceRequest: CreateWorkspaceDto
    ): WorkspaceDto = workspaceService
        .createWorkspace(
            Workspace(
                name = createWorkspaceRequest.name,
                taxEnabled = false,
                multiCurrencyEnabled = true,
                defaultCurrency = createWorkspaceRequest.defaultCurrency,
                ownerId = platformUserService.getCurrentUser().id!!
            )
        )
        .mapToWorkspaceDto(true)

    @PutMapping("workspaces/{workspaceId}")
    suspend fun editWorkspace(
        @RequestBody @Valid editWorkspaceRequest: EditWorkspaceDto,
        @PathVariable workspaceId: Long
    ): WorkspaceDto = workspaceService
        .getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        .apply {
            name = editWorkspaceRequest.name
        }
        .let { workspaceService.save(it) }
        .mapToWorkspaceDto(true)

    @GetMapping("shared-workspaces")
    suspend fun getSharedWorkspaces(): List<WorkspaceDto> = workspaceService
        .getSharedWorkspaces().map { it.mapToWorkspaceDto(false) }

    @PostMapping("shared-workspaces")
    suspend fun saveSharedWorkspace(@Valid @RequestBody request: SaveSharedWorkspaceRequestDto): WorkspaceDto =
        workspaceService.saveSharedWorkspace(request.token).mapToWorkspaceDto(false)
}

data class WorkspaceDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var taxEnabled: Boolean,
    var multiCurrencyEnabled: Boolean,
    var defaultCurrency: String,
    var editable: Boolean
)

data class CreateWorkspaceDto(
    @field:NotBlank var name: String,
    // todo #92: multicurrency is probably redundant; tax to be enabled later
    //@field:NotNull var taxEnabled: Boolean,
    //@field:NotNull var multiCurrencyEnabled: Boolean,
    @field:NotBlank val defaultCurrency: String
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
    taxEnabled = taxEnabled,
    multiCurrencyEnabled = multiCurrencyEnabled,
    defaultCurrency = defaultCurrency,
    editable = editable
)
