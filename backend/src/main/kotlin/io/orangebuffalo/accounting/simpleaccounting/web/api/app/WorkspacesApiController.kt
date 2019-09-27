package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.getCurrentPrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/")
class WorkspacesApiController(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService,
    private val extensions: ApiControllersExtensions
) {

    @GetMapping("workspaces")
    fun getWorkspaces(): Mono<List<WorkspaceDto>> = extensions.toMono {
        val currentPrincipal = getCurrentPrincipal()
        if (currentPrincipal.isTransient) {
            workspaceService
                .getValidWorkspaceAccessToken(currentPrincipal.userName)
                .workspace
                .let { listOf(mapWorkspaceDto(it)) }
        } else {
            workspaceService
                .getUserWorkspaces(currentPrincipal.userName)
                .map { mapWorkspaceDto(it) }
        }
    }

    @PostMapping("workspaces")
    fun createWorkspace(
        @RequestBody @Valid createWorkspaceRequest: CreateWorkspaceDto
    ): Mono<WorkspaceDto> = extensions.toMono {
        workspaceService.createWorkspace(
            Workspace(
                name = createWorkspaceRequest.name,
                taxEnabled = false,
                multiCurrencyEnabled = true,
                defaultCurrency = createWorkspaceRequest.defaultCurrency,
                owner = platformUserService.getCurrentUser()
            )
        ).let { mapWorkspaceDto(it) }
    }

    @PutMapping("workspaces/{workspaceId}")
    fun editWorkspace(
        @RequestBody @Valid editWorkspaceRequest: EditWorkspaceDto,
        @PathVariable workspaceId: Long
    ): Mono<WorkspaceDto> = extensions.toMono {
        extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
            .apply {
                name = editWorkspaceRequest.name
            }
            .let { workspaceService.save(it) }
            .let { mapWorkspaceDto(it) }
    }

    @GetMapping("shared-workspaces")
    fun getSharedWorkspaces(): Mono<List<WorkspaceDto>> = extensions.toMono {
        workspaceService.getSharedWorkspaces().map { mapWorkspaceDto(it) }
    }

    @PostMapping("shared-workspaces")
    fun saveSharedWorkspace(@Valid @RequestBody request: SaveSharedWorkspaceRequestDto): Mono<WorkspaceDto> =
        extensions.toMono {
            mapWorkspaceDto(workspaceService.saveSharedWorkspace(request.token))
        }
}

data class WorkspaceDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var taxEnabled: Boolean,
    var multiCurrencyEnabled: Boolean,
    var defaultCurrency: String
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

private fun mapWorkspaceDto(source: Workspace): WorkspaceDto = WorkspaceDto(
    name = source.name,
    id = source.id,
    version = source.version,
    taxEnabled = source.taxEnabled,
    multiCurrencyEnabled = source.multiCurrencyEnabled,
    defaultCurrency = source.defaultCurrency
)