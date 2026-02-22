package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.ApiPageRequest
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.infra.rest.filtering.NoOpSorting
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/categories")
class CategoriesApi(
    private val categoriesService: CategoriesService,
    private val workspacesService: WorkspacesService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @GetMapping
    suspend fun getCategories(
        @PathVariable workspaceId: Long,
        @ParameterObject request: CategoriesFilteringRequest
    ): ApiPage<CategoryDto> =
        filteringApiExecutor.executeFiltering(request, workspaceId)

    @PostMapping
    suspend fun createCategory(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createCategoryRequest: CreateCategoryDto
    ): CategoryDto = categoriesService
        .createCategory(
            Category(
                name = createCategoryRequest.name,
                workspaceId = workspaceId,
                expense = createCategoryRequest.expense,
                income = createCategoryRequest.income,
                description = createCategoryRequest.description
            )
        )
        .mapToCategoryDto()

    @GetMapping("{categoryId}")
    suspend fun getCategory(
        @PathVariable workspaceId: Long,
        @PathVariable categoryId: Long
    ): CategoryDto {
        workspacesService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val category = categoriesService.getCategoryByIdAndWorkspace(categoryId, workspaceId)
            ?: throw EntityNotFoundException("Category $categoryId is not found")
        return category.mapToCategoryDto()
    }

    @PutMapping("{categoryId}")
    suspend fun updateCategory(
        @PathVariable workspaceId: Long,
        @PathVariable categoryId: Long,
        @RequestBody @Valid request: EditCategoryDto
    ): CategoryDto {
        val category = categoriesService.getCategoryByIdAndWorkspace(categoryId, workspaceId)
            ?: throw EntityNotFoundException("Category $categoryId is not found")

        return category
            .apply {
                name = request.name
                description = request.description
                income = request.income
                expense = request.expense
            }
            .let { categoriesService.saveCategory(it) }
            .mapToCategoryDto()
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder
        .executor<Category, CategoryDto, NoOpSorting, CategoriesFilteringRequest> {
            query(Tables.CATEGORY) {
                addDefaultSorting { root.name.asc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }
            mapper { mapToCategoryDto() }
        }
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

data class EditCategoryDto(
    @field:NotBlank var name: String,
    var description: String?,
    @field:NotNull var income: Boolean,
    @field:NotNull var expense: Boolean
)

private fun Category.mapToCategoryDto() = CategoryDto(
    name = name,
    id = id,
    version = version!!,
    description = description,
    income = income,
    expense = expense
)

class CategoriesFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null
}
