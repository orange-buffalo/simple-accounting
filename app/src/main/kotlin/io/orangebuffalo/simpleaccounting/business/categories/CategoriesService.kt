package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.api.CategoryGqlDto
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.ConnectionGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.CursorPage
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.EdgeGqlDto
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.buildConnection
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.springframework.stereotype.Service

@Service
class CategoriesService(
    private val categoriesRepository: CategoriesRepository,
    private val workspacesService: WorkspacesService
) {

    suspend fun createCategory(category: Category): Category {
        workspacesService.validateWorkspaceAccess(category.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return withDbContext { categoriesRepository.save(category) }
    }

    suspend fun saveCategory(category: Category): Category {
        workspacesService.validateWorkspaceAccess(category.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return withDbContext { categoriesRepository.save(category) }
    }

    suspend fun getCategoryByIdAndWorkspace(categoryId: Long, workspaceId: Long): Category? = withDbContext {
        categoriesRepository.findByIdAndWorkspaceId(categoryId, workspaceId)
    }

    suspend fun validateCategory(categoryId: Long, workspaceId: Long) = withDbContext {
        if (!categoriesRepository.existsByIdAndWorkspaceId(categoryId, workspaceId)) {
            throw EntityNotFoundException("Category $categoryId is not found")
        }
    }

    suspend fun getCategoriesPaginated(
        workspaceId: Long,
        first: Int,
        cursorPage: CursorPage,
    ): ConnectionGqlDto<CategoryGqlDto> = withDbContext {
        val items = categoriesRepository.findByWorkspaceIdPaginated(
            workspaceId = workspaceId,
            limit = first,
            afterCreatedAt = cursorPage.createdAtAfter,
        )
        val totalCount = categoriesRepository.countByWorkspaceId(workspaceId)
        buildConnection(
            items = items,
            requestedPageSize = first,
            totalCount = totalCount,
            cursorPage = cursorPage,
            mapper = { category ->
                EdgeGqlDto(
                    cursor = encodeCursor(category.createdAt!!),
                    node = CategoryGqlDto(
                        id = category.id!!.toInt(),
                        name = category.name,
                        description = category.description,
                        income = category.income,
                        expense = category.expense,
                    ),
                )
            },
        )
    }
}
