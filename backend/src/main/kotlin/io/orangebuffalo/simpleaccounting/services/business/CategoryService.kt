package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CategoryRepository
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
