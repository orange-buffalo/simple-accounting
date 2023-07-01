package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.CategoryService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.*
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/categories")
class CategoriesApiController(
    private val categoryService: CategoryService,
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
    ): CategoryDto = categoryService
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
