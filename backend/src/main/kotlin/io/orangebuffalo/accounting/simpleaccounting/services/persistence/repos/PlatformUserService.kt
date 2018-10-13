package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class PlatformUserService(
        private val userRepository: PlatformUserRepository
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

}