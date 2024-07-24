package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val workspaceService: WorkspaceService
) {

    suspend fun createCategory(category: Category): Category {
        workspaceService.validateWorkspaceAccess(category.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return withDbContext { categoryRepository.save(category) }
    }

    suspend fun validateCategory(categoryId: Long, workspaceId: Long) = withDbContext {
        if (!categoryRepository.existsByIdAndWorkspaceId(categoryId, workspaceId)) {
            throw EntityNotFoundException("Category $categoryId is not found")
        }
    }
}
