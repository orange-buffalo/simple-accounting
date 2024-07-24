package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class GeneralTaxService(
    private val repository: GeneralTaxRepository,
    private val workspaceService: WorkspaceService
) {

    suspend fun saveTax(tax: GeneralTax): GeneralTax {
        workspaceService.getAccessibleWorkspace(tax.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return withDbContext { repository.save(tax) }
    }

    suspend fun getTaxByIdAndWorkspace(id: Long, workspaceId: Long): GeneralTax? = withDbContext {
        repository.findByIdAndWorkspaceId(id, workspaceId)
    }

    suspend fun getValidGeneralTax(taxId: Long, workspaceId: Long): GeneralTax? = withDbContext {
        repository.findByIdAndWorkspaceId(taxId, workspaceId)
            ?: throw EntityNotFoundException("Tax $taxId is not found")
    }

    suspend fun validateGeneralTax(taxId: Long, workspaceId: Long) = withDbContext {
        if (!repository.existsByIdAndWorkspaceId(taxId, workspaceId)) {
            throw EntityNotFoundException("Tax $taxId is not found")
        }
    }
}
