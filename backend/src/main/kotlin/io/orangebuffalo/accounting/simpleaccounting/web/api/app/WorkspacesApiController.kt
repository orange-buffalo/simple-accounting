package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping.ApiDtoMapperAdapter
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/v1/user/workspaces")
class WorkspacesApiController(
        private val mapper: WorkspaceDtoMapper,
        private val platformUserService: PlatformUserService
) {

    @GetMapping
    fun getWorkspaces(principal: Mono<Principal>): Flux<WorkspaceDto> {
        return principal.flatMapMany { platformUserService.getUserWorkspaces(it.name) }
                .map { mapper.map(it) }
    }

    @PostMapping
    fun createWorkspace(
            principal: Mono<Principal>,
            @RequestBody createWorkspaceRequest: Mono<CreateWorkspaceDto>
    ): Mono<WorkspaceDto> = principal
            .flatMap { platformUserService.getUserByUserName(it.name) }
            .flatMap { owner ->
                createWorkspaceRequest.map {
                    Workspace(name = it.name,
                            taxEnabled = it.taxEnabled,
                            multiCurrencyEnabled = it.multiCurrencyEnabled,
                            defaultCurrency = it.defaultCurrency,
                            owner = owner)
                }.flatMap { platformUserService.createWorkspace(it) }
            }
            .map { mapper.map(it) }
}

data class WorkspaceDto(
        var id: Long?,
        var version: Int,
        var name: String,
        var taxEnabled: Boolean,
        var multiCurrencyEnabled: Boolean,
        val defaultCurrency: String)

data class CreateWorkspaceDto(
        @field:NotBlank var name: String,
        @field:NotNull var taxEnabled: Boolean,
        @field:NotNull var multiCurrencyEnabled: Boolean,
        @field:NotBlank val defaultCurrency: String)

@Component
class WorkspaceDtoMapper
    : ApiDtoMapperAdapter<Workspace, WorkspaceDto>(Workspace::class.java, WorkspaceDto::class.java) {

    override fun map(source: Workspace): WorkspaceDto = WorkspaceDto(
            name = source.name,
            id = source.id,
            version = source.version,
            taxEnabled = source.taxEnabled,
            multiCurrencyEnabled = source.multiCurrencyEnabled,
            defaultCurrency = source.defaultCurrency)
}