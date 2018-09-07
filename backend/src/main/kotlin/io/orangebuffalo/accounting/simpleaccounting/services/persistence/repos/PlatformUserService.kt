package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PlatformUserService(
        private val userRepository: PlatformUserRepository
) {

    fun getUserByUserName(userName: String): Mono<PlatformUser> {
        return Mono.fromSupplier { userRepository.findByUserName(userName) }
                //TODO we should use a dedicated pool here
//                .subscribeOn()
    }

}