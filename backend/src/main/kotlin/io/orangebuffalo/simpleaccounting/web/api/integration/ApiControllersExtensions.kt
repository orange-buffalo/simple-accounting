package io.orangebuffalo.simpleaccounting.web.api.integration

import io.orangebuffalo.simpleaccounting.services.business.CustomerService
import io.orangebuffalo.simpleaccounting.services.business.GeneralTaxService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.DocumentRepository
import org.springframework.stereotype.Component

@Component
class ApiControllersExtensions(
    private val documentRepository: DocumentRepository,
    private val customerService: CustomerService,
    private val generalTaxService: GeneralTaxService
) {

    suspend fun getValidGeneralTax(taxId: Long?, workspace: Workspace): GeneralTax? =
        if (taxId == null) {
            null
        } else {
            generalTaxService.getTaxByIdAndWorkspace(taxId, workspace)
                ?: throw EntityNotFoundException(
                    "Tax $taxId is not found"
                )
        }

    suspend fun getValidDocuments(
        workspace: Workspace,
        documentIds: List<Long>?
    ): Set<Document> {
        val documents = documentIds?.let { documentRepository.findAllById(it) } ?: emptyList()
        documents.forEach { document ->
            if (document.workspace != workspace) {
                throw EntityNotFoundException("Document ${document.id} is not found")
            }
        }
        return documents.toSet()
    }

    fun getValidCategory(
        workspace: Workspace,
        categoryId: Long?
    ): Category? {
        if (categoryId == null) {
            return null
        }

        return workspace.categories.asSequence()
            .firstOrNull { workspaceCategory -> workspaceCategory.id == categoryId }
            ?: throw EntityNotFoundException("Category $categoryId is not found")
    }

    suspend fun getValidCustomer(workspace: Workspace, customerId: Long): Customer =
        customerService.getCustomerByIdAndWorkspace(customerId, workspace)
            ?: throw EntityNotFoundException("Customer $customerId is not found")
}
