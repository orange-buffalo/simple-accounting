package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class GeneralTaxesService(
    private val repository: GeneralTaxesRepository,
    private val workspacesService: WorkspacesService
) {

    fun saveTax(tax: GeneralTax): GeneralTax {
        workspacesService.getAccessibleWorkspace(tax.workspaceId, WorkspaceAccessMode.READ_WRITE)
        return repository.save(tax)
    }

    fun getTaxByIdAndWorkspace(id: String, workspaceId: String): GeneralTax? =
        repository.findByIdAndWorkspaceId(id, workspaceId)

    fun getValidGeneralTax(taxId: String, workspaceId: String): GeneralTax? =
        repository.findByIdAndWorkspaceId(taxId, workspaceId)
            ?: throw EntityNotFoundException("Tax $taxId is not found")

    fun validateGeneralTax(taxId: String, workspaceId: String) {
        if (!repository.existsByIdAndWorkspaceId(taxId, workspaceId)) {
            throw EntityNotFoundException("Tax $taxId is not found")
        }
    }
}
