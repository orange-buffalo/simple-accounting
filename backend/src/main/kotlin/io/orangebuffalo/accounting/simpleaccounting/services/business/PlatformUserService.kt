package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.PlatformUserRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class PlatformUserService(
        private val userRepository: PlatformUserRepository,
        private val workspaceRepository: WorkspaceRepository
) {

    fun getUserByUserName(userName: String): Mono<PlatformUser> {
        return Mono.fromSupplier { userRepository.findByUserName(userName) }
                .subscribeOn(Schedulers.elastic())
                .filter { it.isPresent }
                .map { it.get() }
    }

    fun getUsers(page: Pageable): Mono<Page<PlatformUser>> {
        return Mono.fromSupplier { userRepository.findAll(page) }
                .subscribeOn(Schedulers.elastic())
    }

    fun save(user: PlatformUser): Mono<PlatformUser> {
        return Mono.fromCallable { userRepository.save(user) }
                .subscribeOn(Schedulers.elastic())
    }

    fun getUserWorkspaces(userName: String): Flux<Workspace> {
        return Flux.fromStream { workspaceRepository.findAllByOwnerUserName(userName).stream() }
                .subscribeOn(Schedulers.elastic())
    }

}