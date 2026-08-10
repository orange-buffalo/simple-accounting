package io.orangebuffalo.simpleaccounting.business.categories

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class CategoriesService(
    private val categoriesRepository: CategoriesRepository,
    private val workspacesService: WorkspacesService
) {

    fun createCategory(category: Category): Category {
        workspacesService.validateWorkspaceAccess(category.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return categoriesRepository.save(category)
    }

    fun saveCategory(category: Category): Category {
        workspacesService.validateWorkspaceAccess(category.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return categoriesRepository.save(category)
    }

    fun getCategoryByIdAndWorkspace(categoryId: String, workspaceId: String): Category? =
        categoriesRepository.findByIdAndWorkspaceId(categoryId, workspaceId)

    fun validateCategory(categoryId: String, workspaceId: String) {
        if (!categoriesRepository.existsByIdAndWorkspaceId(categoryId, workspaceId)) {
            throw EntityNotFoundException("Category $categoryId is not found")
        }
    }
}
