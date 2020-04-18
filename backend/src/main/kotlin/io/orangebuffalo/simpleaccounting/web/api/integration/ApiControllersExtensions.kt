package io.orangebuffalo.simpleaccounting.web.api.integration

import io.orangebuffalo.simpleaccounting.services.business.CategoryService
import io.orangebuffalo.simpleaccounting.services.business.CustomerService
import io.orangebuffalo.simpleaccounting.services.business.DocumentsService
import io.orangebuffalo.simpleaccounting.services.business.GeneralTaxService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import org.springframework.stereotype.Component

@Component
class ApiControllersExtensions(
    private val documentsService: DocumentsService,
    private val customerService: CustomerService,
    private val generalTaxService: GeneralTaxService,
    private val categoryService: CategoryService
) {

    suspend fun getValidGeneralTax(taxId: Long?, workspace: Workspace): GeneralTax? =
        generalTaxService.getValidGeneralTax(taxId, workspace)

    suspend fun getValidDocuments(
        workspace: Workspace,
        documentIds: List<Long>?
    ): Set<Document> = documentsService.getValidDocuments(workspace, documentIds)

    fun getValidCategory(
        workspace: Workspace,
        categoryId: Long?
    ): Category? = categoryService.getValidCategory(workspace, categoryId)

    suspend fun getValidCustomer(workspace: Workspace, customerId: Long): Customer =
        customerService.getCustomerByIdAndWorkspace(customerId, workspace)
            ?: throw EntityNotFoundException("Customer $customerId is not found")
}
