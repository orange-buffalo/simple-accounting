package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class GeneralTaxesService(
    private val repository: GeneralTaxesRepository,
    private val workspacesService: WorkspacesService
) {

    suspend fun saveTax(tax: GeneralTax): GeneralTax {
        workspacesService.getAccessibleWorkspace(tax.workspaceId, WorkspaceAccessMode.READ_WRITE)
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
