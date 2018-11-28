package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import kotlinx.coroutines.Deferred
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class PlatformUserService(
    private val userRepository: PlatformUserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend fun getCurrentUserAsync(): Deferred<PlatformUser> = withDbContextAsync {
        userRepository.findByUserName(getCurrentPrincipal().username)
    }

    @Deprecated("migrate to coroutines")
    fun getUserByUserName(userName: String): Mono<PlatformUser> {
        return Mono.fromSupplier { userRepository.findByUserName(userName) }
            .subscribeOn(Schedulers.elastic())
            .filter { it != null }
    }

    @Deprecated("migrate to coroutines")
    fun getUsers(page: Pageable): Mono<Page<PlatformUser>> {
        return Mono.fromSupplier { userRepository.findAll(page) }
            .subscribeOn(Schedulers.elastic())
    }

    @Deprecated("migrate to coroutines")
    fun save(user: PlatformUser): Mono<PlatformUser> {
        return Mono.fromCallable { userRepository.save(user) }
            .subscribeOn(Schedulers.elastic())
    }

    @Deprecated("migrate to coroutines")
    fun getUserWorkspaces(userName: String): Flux<Workspace> {
        return Flux.fromStream { workspaceRepository.findAllByOwnerUserName(userName).stream() }
            .subscribeOn(Schedulers.elastic())
    }

    @Deprecated("migrate to coroutines")
    fun createWorkspace(workspace: Workspace): Mono<Workspace> {
        return Mono.fromCallable { workspaceRepository.save(workspace) }
            .subscribeOn(Schedulers.elastic())
    }

    @Deprecated("migrate to coroutines")
    fun getUserCategories(userName: String): Flux<Category> {
        return Flux.fromStream { categoryRepository.findAllByWorkspaceOwnerUserName(userName).stream() }
            .subscribeOn(Schedulers.elastic())
    }
}