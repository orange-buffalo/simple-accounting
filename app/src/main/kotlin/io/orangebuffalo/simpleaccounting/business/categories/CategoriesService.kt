package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
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

    suspend fun validateCategory(categoryId: Long, workspaceId: Long) = withDbContext {
        if (!categoriesRepository.existsByIdAndWorkspaceId(categoryId, workspaceId)) {
            throw EntityNotFoundException("Category $categoryId is not found")
        }
    }
}
