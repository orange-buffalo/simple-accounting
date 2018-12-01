package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import io.orangebuffalo.accounting.simpleaccounting.services.business.CoroutinePrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
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
    private val workspaceService: WorkspaceService
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
}
