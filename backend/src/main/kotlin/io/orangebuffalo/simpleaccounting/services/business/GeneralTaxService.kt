package io.orangebuffalo.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QGeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.services.persistence.repos.GeneralTaxRepository
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GeneralTaxService(
    private val repository: GeneralTaxRepository
) {

    suspend fun saveTax(tax: GeneralTax): GeneralTax {
        return withDbContext {
            repository.save(tax)
        }
    }

    suspend fun getTaxes(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<GeneralTax> = withDbContext {
        repository.findAll(QGeneralTax.generalTax.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getTaxByIdAndWorkspace(id: Long, workspace: Workspace): GeneralTax? =
        withDbContext {
            repository.findByIdAndWorkspace(id, workspace)
        }

    suspend fun getValidGeneralTax(taxId: Long?, workspace: Workspace): GeneralTax? =
        if (taxId == null) {
            null
        } else {
            getTaxByIdAndWorkspace(taxId, workspace) ?: throw EntityNotFoundException("Tax $taxId is not found")
        }
}
