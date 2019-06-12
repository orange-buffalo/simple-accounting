package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QCategory
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    suspend fun createCategory(category: Category): Category =
        withDbContext {
            categoryRepository.save(category)
        }

    suspend fun getCategories(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Category> = withDbContext {
        categoryRepository.findAll(QCategory.category.workspace.eq(workspace).and(filter), page)
    }

}