package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.CategoryService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/categories")
class CategoriesApiController(
    private val categoryService: CategoryService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @GetMapping
    suspend fun getCategories(@PathVariable workspaceId: Long): ApiPage<CategoryDto> =
        filteringApiExecutor.executeFiltering(workspaceId)

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

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<Category, CategoryDto> {
        query(Tables.CATEGORY) {
            addDefaultSorting { root.id.desc() }
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
