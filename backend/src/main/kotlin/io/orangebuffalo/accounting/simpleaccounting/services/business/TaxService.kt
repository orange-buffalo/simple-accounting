package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QTax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Tax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.TaxRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TaxService(
    private val taxRepository: TaxRepository
) {

    suspend fun saveTax(tax: Tax): Tax {
        return withDbContext {
            taxRepository.save(tax)
        }
    }

    suspend fun getTaxes(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Tax> = withDbContext {
        taxRepository.findAll(QTax.tax.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getTaxByIdAndWorkspace(id: Long, workspace: Workspace): Tax? =
        withDbContext {
            taxRepository.findByIdAndWorkspace(id, workspace)
        }
}