package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.CoroutinePrincipal
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
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

    suspend fun getAccessibleWorkspace(workspaceId: Long): Workspace {
        val currentUser = platformUserService.getCurrentUserAsync()
        val workspace = workspaceService.getWorkspaceAsync(workspaceId).await()
            ?: throw ApiValidationException("Workspace $workspaceId cannot be found")
        return if (workspace.owner == currentUser.await()) {
            workspace
        } else {
            throw ApiValidationException("Workspace $workspaceId cannot be found")
        }
    }

    @Deprecated("migrate to coroutines")
    fun <T> withAccessibleWorkspace(
        workspaceId: Long,
        consumer: (workspace: Workspace) -> Mono<T>
    ): Mono<T> = withCurrentUser { currentUser ->
        workspaceService.getWorkspace(workspaceId)
            .flatMap { workspace ->
                if (workspace.owner == currentUser) {
                    Mono.just(workspace)
                } else {
                    Mono.error<Workspace>(ApiValidationException("Workspace $workspaceId cannot be found"))
                }
            }
            .flatMap { consumer(it) }
    }

    @Deprecated("migrate to coroutines")
    fun <T> withCurrentUser(
        consumer: (currentUser: PlatformUser) -> Mono<T>
    ): Mono<T> = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication.principal }
        .cast(UserDetails::class.java)
        .flatMap { platformUserService.getUserByUserName(it.username) }
        .flatMap { currentUser -> consumer(currentUser) }

    fun <T> toMono(block: suspend () -> T): Mono<T> = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication.principal }
        .cast(UserDetails::class.java)
        .flatMap { principal ->
            GlobalScope.mono(CoroutinePrincipal(principal)) {
                block()
            }
        }
}
