package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContextAsync
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import kotlinx.coroutines.Deferred
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend fun getWorkspaceAsync(workspaceId: Long): Deferred<Workspace?> =
        withDbContextAsync {
            workspaceRepository.findById(workspaceId).orElse(null)
        }

    suspend fun createCategory(category: Category): Category =
        withDbContext {
            categoryRepository.save(category)
        }
}