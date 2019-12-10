package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.CategoryService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QCategory
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/categories")
class CategoriesApiController(
    private val categoryService: CategoryService,
    private val workspaceService: WorkspaceService
) {

    @GetMapping
    @PageableApi(CategoryPageableApiDescriptor::class)
    suspend fun getCategories(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Page<Category> = workspaceService
        .getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        .let { workspace ->
            categoryService.getCategories(workspace, pageRequest.page, pageRequest.predicate)
        }

    @PostMapping
    suspend fun createCategory(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createCategoryRequest: CreateCategoryDto
    ): CategoryDto = workspaceService
        .getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_WRITE)
        .let { workspace ->
            categoryService.createCategory(
                Category(
                    name = createCategoryRequest.name,
                    workspace = workspace,
                    expense = createCategoryRequest.expense,
                    income = createCategoryRequest.income,
                    description = createCategoryRequest.description
                )
            )
        }
        .let { mapCategoryDto(it) }
}

data class CategoryDto(
    var id: Long?,
    var version: Int,
    var name: String,
    var description: String?,
    var income: Boolean,
    var expense: Boolean
)

data class CreateCategoryDto(
    @field:NotBlank var name: String,
    var description: String?,
    @field:NotNull var income: Boolean,
    @field:NotNull var expense: Boolean
)

private fun mapCategoryDto(source: Category) = CategoryDto(
    name = source.name,
    id = source.id,
    version = source.version,
    description = source.description,
    income = source.income,
    expense = source.expense
)

@Component
class CategoryPageableApiDescriptor : PageableApiDescriptor<Category, QCategory> {
    override suspend fun mapEntityToDto(entity: Category) =
        mapCategoryDto(entity)
}
