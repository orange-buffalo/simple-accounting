package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import io.orangebuffalo.accounting.simpleaccounting.services.business.CoroutinePrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentService
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ApiControllersExtensions(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService,
    private val documentService: DocumentService
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

    fun <T> toMono(block: suspend CoroutineScope.() -> T): Mono<T> = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication.principal }
        .cast(UserDetails::class.java)
        .flatMap { principal ->
            GlobalScope.mono(CoroutinePrincipal(principal)) {
                block()
            }
        }

    suspend fun getValidDocuments(
        workspace: Workspace,
        documentIds: List<Long>?
    ): List<Document> {
        val documents = documentIds?.let { documentService.getDocumentsByIds(it) } ?: emptyList()
        documents.forEach { document ->
            if (document.workspace != workspace) {
                throw EntityNotFoundException("Document ${document.id} is not found")
            }
        }
        return documents
    }

    fun getValidCategory(
        workspace: Workspace,
        categoryId: Long
    ) = workspace.categories.asSequence()
        .firstOrNull { workspaceCategory -> workspaceCategory.id == categoryId }
        ?: throw EntityNotFoundException("Category $categoryId is not found")
}
