package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/v1/user/workspaces")
class WorkspacesApiController(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService
) {

    @GetMapping
    fun getWorkspaces(principal: Mono<Principal>): Flux<WorkspaceDto> = principal
        .map { it.name }
        .flatMapMany { userName ->
            val categories = platformUserService.getUserCategories(userName)
                .map { category -> Triple(category.workspace, category, null) }

            val taxes = Flux.empty<Triple<Workspace, Category?, Any?>>()

            val workspaces = platformUserService.getUserWorkspaces(userName)
                .map { workspace -> Triple(workspace, null, null) }

            Flux.merge(categories, workspaces, taxes)
                .groupBy { triple -> triple.first }
                .flatMap { fluxOfTripleByWorkspace ->
                    fluxOfTripleByWorkspace
                        .collectList()
                        .map { triples ->
                            mapWorkspaceDto(
                                fluxOfTripleByWorkspace.key()!!,
                                triples.asSequence()
                                    .map { it.second }
                                    .filterNotNull()
                                    .map(::mapCategoryDto)
                                    .toList()
                            )
                        }
                }
        }

    @PostMapping
    fun createWorkspace(
        principal: Mono<Principal>,
        @RequestBody @Valid createWorkspaceRequest: Mono<CreateWorkspaceDto>
    ): Mono<WorkspaceDto> = principal
        .flatMap { platformUserService.getUserByUserName(it.name) }
        .flatMap { owner ->
            createWorkspaceRequest.map {
                Workspace(
                    name = it.name,
                    taxEnabled = it.taxEnabled,
                    multiCurrencyEnabled = it.multiCurrencyEnabled,
                    defaultCurrency = it.defaultCurrency,
                    owner = owner
                )
            }.flatMap { platformUserService.createWorkspace(it) }
        }
        .map { mapWorkspaceDto(it, emptyList()) }

    @PostMapping("/{workspaceId}/categories")
    fun createCategory(
        principal: Mono<Principal>,
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createCategoryRequest: Mono<CreateCategoryDto>
    ): Mono<CategoryDto> = principal
        .flatMap { platformUserService.getUserByUserName(it.name) }
        .flatMap { currentUser ->
            workspaceService.getWorkspace(workspaceId)
                .flatMap { workspace ->
                    if (workspace.owner == currentUser) {
                        createCategoryRequest
                            .flatMap { categoryRequest ->
                                workspaceService.createCategory(
                                    Category(
                                        name = categoryRequest.name,
                                        workspace = workspace,
                                        expense = categoryRequest.expense,
                                        income = categoryRequest.income,
                                        description = categoryRequest.description
                                    )
                                )
                            }
                            .map(::mapCategoryDto)
                    } else {
                        Mono.defer {
                            Mono.error<CategoryDto>(ApiValidationException("Workspace $workspaceId cannot be found"))
                        }
                    }
                }
        }
}

data class WorkspaceDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var taxEnabled: Boolean,
    var multiCurrencyEnabled: Boolean,
    var defaultCurrency: String,
    var categories: List<CategoryDto> = emptyList()
)

data class CategoryDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var description: String?,
    var income: Boolean,
    var expense: Boolean
)

data class CreateWorkspaceDto(
    @field:NotBlank var name: String,
    @field:NotNull var taxEnabled: Boolean,
    @field:NotNull var multiCurrencyEnabled: Boolean,
    @field:NotBlank val defaultCurrency: String
)

data class CreateCategoryDto(
    @field:NotBlank var name: String,
    var description: String?,
    @field:NotNull var income: Boolean,
    @field:NotNull var expense: Boolean
)

private fun mapWorkspaceDto(source: Workspace, categories: List<CategoryDto>): WorkspaceDto = WorkspaceDto(
    name = source.name,
    id = source.id,
    version = source.version,
    taxEnabled = source.taxEnabled,
    multiCurrencyEnabled = source.multiCurrencyEnabled,
    defaultCurrency = source.defaultCurrency,
    categories = categories
)

private fun mapCategoryDto(source: Category) = CategoryDto(
    name = source.name,
    id = source.id,
    version = source.version,
    description = source.description,
    income = source.income,
    expense = source.expense
)