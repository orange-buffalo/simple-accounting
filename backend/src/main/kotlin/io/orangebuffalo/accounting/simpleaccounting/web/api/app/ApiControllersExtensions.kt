package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ApiControllersExtensions(
    private val platformUserService: PlatformUserService,
    private val workspaceService: WorkspaceService
) {

    fun <T> withAccessibleWorkspace(
        workspaceId: Long,
        consumer: (workspace: Workspace) -> Mono<T>
    ): Mono<T> = withCurrentUser { currentUser ->
        workspaceService.getWorkspace(workspaceId)
            .flatMap { workspace ->
                if (workspace.owner == currentUser) {
                    consumer(workspace)
                } else {
                    Mono.empty()
                }
            }
            .switchIfEmpty(Mono.defer {
                Mono.error<T>(ApiValidationException("Workspace $workspaceId cannot be found"))
            })
    }

    fun <T> withCurrentUser(
        consumer: (currentUser: PlatformUser) -> Mono<T>
    ): Mono<T> = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication.principal }
        .cast(UserDetails::class.java)
        .flatMap { platformUserService.getUserByUserName(it.username) }
        .flatMap { currentUser -> consumer(currentUser) }
}
