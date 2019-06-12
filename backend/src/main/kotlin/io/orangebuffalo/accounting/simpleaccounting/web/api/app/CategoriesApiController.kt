package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.CategoryService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QCategory
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/categories")
class CategoriesApiController(
    private val categoryService: CategoryService,
    private val extensions: ApiControllersExtensions
) {

    @GetMapping
    @PageableApi(CategoryPageableApiDescriptor::class)
    fun getCategories(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Category>> = extensions.toMono {
        extensions.getAccessibleWorkspace(workspaceId)
            .let { workspace ->
                categoryService.getCategories(workspace, pageRequest.page, pageRequest.predicate)
            }
    }

    @PostMapping
    fun createCategory(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createCategoryRequest: CreateCategoryDto
    ): Mono<CategoryDto> = extensions.toMono {
        extensions.getAccessibleWorkspace(workspaceId)
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
    override suspend fun mapEntityToDto(entity: Category) = mapCategoryDto(entity)
}