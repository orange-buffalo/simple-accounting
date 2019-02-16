package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import io.orangebuffalo.accounting.simpleaccounting.services.business.*
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ApiControllersExtensions(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService,
    private val documentService: DocumentService,
    private val customerService: CustomerService,
    private val taxService: TaxService
) {

    suspend fun validateWorkspaceAccess(workspaceId: Long) {
        getAccessibleWorkspace(workspaceId)
    }

    suspend fun getAccessibleWorkspace(workspaceId: Long): Workspace {
        val currentUser = platformUserService.getCurrentUserAsync()
        val workspace = workspaceService.getWorkspaceAsync(workspaceId).await()
            ?: throw EntityNotFoundException("Workspace $workspaceId is not found")
        return if (workspace.owner == currentUser.await()) {
            workspace
        } else {
            throw EntityNotFoundException("Workspace $workspaceId is not found")
        }
    }

    suspend fun getValidTax(taxId: Long?, workspace: Workspace): Tax? =
        if (taxId == null) {
            null
        } else {
            taxService.getTaxByIdAndWorkspace(taxId, workspace)
                ?: throw EntityNotFoundException("Tax $taxId is not found")
        }

    //todo remove
    fun <T> toMono(block: suspend CoroutineScope.() -> T): Mono<T> =
        io.orangebuffalo.accounting.simpleaccounting.services.integration.toMono(block)

    suspend fun getValidDocuments(
        workspace: Workspace,
        documentIds: List<Long>?
    ): Set<Document> {
        val documents = documentIds?.let { documentService.getDocumentsByIds(it) } ?: emptyList()
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
